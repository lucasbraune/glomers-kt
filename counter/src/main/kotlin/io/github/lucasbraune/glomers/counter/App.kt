package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.ErrorCodes
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.Log
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.ResponseBody
import io.github.lucasbraune.glomers.protocol.RpcError
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.rpc
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.glomers.services.seqkv.CompareAndSet
import io.github.lucasbraune.glomers.services.seqkv.CompareAndSetOk
import io.github.lucasbraune.glomers.services.seqkv.SEQ_KV_NODE_ID
import io.github.lucasbraune.glomers.services.seqkv.Write
import io.github.lucasbraune.glomers.services.seqkv.WriteOk
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.lucasbraune.glomers.services.seqkv.Read as KvRead
import io.github.lucasbraune.glomers.services.seqkv.ReadOk as KvReadOk

class Node {
    private val io = NodeIO(
        inputSerializers = CounterInputSerializers,
        outputSerializers = CounterOutputSerializers,
    )
    private val initService = InitService()
    private val client = Client(io, initService)
    private val msgId = AtomicInteger()
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
            write(INITIAL_VALUE)
        }
        var remoteValue = INITIAL_VALUE
        while (true) {
            delay(syncPeriod)
            try {
                val oldRemoteValue = remoteValue
                remoteValue = read() ?: continue
                val newLocalValue = value.addAndGet(remoteValue - oldRemoteValue)
                if (remoteValue != newLocalValue) {
                    val error = cas(remoteValue, newLocalValue)
                    if (error == null) {
                        remoteValue = newLocalValue
                    } else {
                        Log.error("CAS failed with error code $error")
                    }
                }
            } catch (exception: Throwable) {
                Log.error(exception)
            }
        }
    }

    private fun nextMsgId() = msgId.getAndIncrement()

    private suspend fun read(): Int? {
        val request = KvRead(nextMsgId(), COUNTER_KEY)
        val response = client.rpc<ResponseBody>(SEQ_KV_NODE_ID, request)
        return when (response) {
            is KvReadOk -> response.value
            is RpcError -> {
                if (response.code == ErrorCodes.KEY_DOES_NOT_EXIST) {
                    null
                } else {
                    throw Exception("Kv-Read failed with error $response")
                }
            }
            else -> throw Exception("Unexpected response: $response")
        }
    }

    private suspend fun write(value: Int) {
        val request = Write(nextMsgId(), COUNTER_KEY, value)
        client.rpc<WriteOk>(SEQ_KV_NODE_ID, request)
    }

    /**
     * Returns error code, if any.
     * @see ErrorCodes.KEY_DOES_NOT_EXIST
     * @see ErrorCodes.PRECONDITION_FAILED
     * */
    private suspend fun cas(from: Int, to: Int): Int? {
        val request = CompareAndSet(nextMsgId(), COUNTER_KEY, from, to)
        val response = client.rpc<ResponseBody>(SEQ_KV_NODE_ID, request)
        return when (response) {
            is CompareAndSetOk -> null
            is RpcError -> response.code
            else -> throw Exception("Unexpected response: $response")
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
