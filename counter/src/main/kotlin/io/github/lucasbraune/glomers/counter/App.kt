package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.Log
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.RpcException
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.glomers.seqkv.SeqKvStore
import io.github.lucasbraune.util.RetryOptions
import io.github.lucasbraune.util.retry
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Node {
    private val io = NodeIO(
        inputSerializers = CounterInputSerializers,
        outputSerializers = CounterOutputSerializers,
    )
    private val initService = InitService()
    private val client = Client(io, initService)
    private val msgId = AtomicInteger()
    private val kvStore = SeqKvStore<String, Map<String, Int>>(client, msgId::getAndIncrement)
    /** Sum of values in [Add] requests to this node. */
    private val localSum = AtomicInteger()
    /** Sum of values in [Add] requests to other nodes. */
    private val remoteSum = AtomicInteger()

    suspend fun serve() = serve(io) {
        request(initService::handle)
        message(client::handle)
        request<Add> {
            localSum.addAndGet(it.body.delta)
            AddOk(it.body.msgId)
        }
        request<Read> {
            ReadOk(it.body.msgId, localSum.get() + remoteSum.get())
        }
    }

    suspend fun sync() {
        val nodeId = initService.nodeId()
        val nodeIds = initService.nodeIds()
        if (nodeId == nodeIds.first()) {
            retry(retryOptions) {
                kvStore.write(KEY, nodeIds.associateWith { 0 })
            }
        }
        while (true) {
            delay(syncPeriod)
            try {
                // Pull
                val sums = kvStore.read(KEY)
                val newRemoteSum = sums.filter { it.key != nodeId }.values.sum()
                remoteSum.set(newRemoteSum)
                // Push
                val newSums = buildMap {
                    putAll(sums)
                    put(nodeId, localSum.get())
                }
                kvStore.compareAndSet(KEY, sums, newSums)
            } catch (exception: RpcException) {
                Log.error(exception)
            }
        }
    }

    private companion object {
        const val KEY = "counter"
        val syncPeriod = 300.milliseconds
        val retryOptions = RetryOptions(3, backoff = 200.milliseconds)
    }
}

suspend fun main() = coroutineScope {
    val node = Node()
    launch { node.sync() }
    node.serve()
}
