package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.modules.plus

class Node {
    private val io = NodeIO(InitSerializersModule + CounterSerializersModule)
    private val initService = InitService()
    private val value = AtomicInteger()

    suspend fun serve() = serve(io) {
        request(initService::handle)
        request<Add> {
            value.addAndGet(it.body.delta)
            // Try to update value in KV store
            AddOk(it.body.msgId)
        }
        request<Read> {
            // Update value reading from KV store
            ReadOk(it.body.msgId, value.get())
        }
    }
}

suspend fun main() {
    val node = Node()
    node.serve()
}
