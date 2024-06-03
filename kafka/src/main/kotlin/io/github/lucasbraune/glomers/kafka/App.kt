package io.github.lucasbraune.glomers.kafka

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.coroutineScope

class Node {
    private val io = NodeIO(KafkaSerializers)
    private val initService = InitService()
    private val client = Client(io, initService)
    private val msgId = AtomicInteger()

    suspend fun serve() = serve(io) {
        request(initService::handle)
        message(client::handle)
        request<Send> {
            TODO()
        }
        request<Poll> {
            TODO()
        }
        request<CommitOffsets> {
            TODO()
        }
        request<ListCommittedOffsets> {
            TODO()
        }
    }
}

suspend fun main() = coroutineScope {
    val node = Node()
    node.serve()
}
