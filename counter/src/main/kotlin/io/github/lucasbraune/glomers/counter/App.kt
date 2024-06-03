package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.glomers.seqkv.SeqKvStore
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
    private val kvStore = SeqKvStore<String, Int>(client, msgId::getAndIncrement)
    private val value = AtomicInteger(INITIAL_VALUE)

    suspend fun serve() = serve(io) {
        request(initService::handle)
        message(client::handle)
        request<Add> {
            value.addAndGet(it.body.delta)
            AddOk(it.body.msgId)
        }
        request<Read> {
            ReadOk(it.body.msgId, value.get())
        }
    }

    suspend fun sync() {
        val shouldInitKvStore = initService.nodeId() == initService.nodeIds().first()
        if (shouldInitKvStore) {
            kvStore.write(COUNTER_KEY, INITIAL_VALUE)
        }
        var remoteValue = INITIAL_VALUE
        while (true) {
            delay(syncPeriod)
            runCatching {
                val oldRemoteValue = remoteValue
                remoteValue = kvStore.read(COUNTER_KEY)
                val newLocalValue = value.addAndGet(remoteValue - oldRemoteValue)
                if (remoteValue != newLocalValue) {
                    kvStore.compareAndSet(COUNTER_KEY, remoteValue, newLocalValue)
                    remoteValue = newLocalValue
                }
            }
        }
    }

    private companion object {
        const val COUNTER_KEY = "counter"
        const val INITIAL_VALUE = 0
        val syncPeriod = 300.milliseconds
    }
}

suspend fun main() = coroutineScope {
    val node = Node()
    launch { node.sync() }
    node.serve()
}
