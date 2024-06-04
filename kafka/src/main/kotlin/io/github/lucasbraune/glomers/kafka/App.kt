package io.github.lucasbraune.glomers.kafka

import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.util.withSingleThreadContext
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope

class Node {
    private val io = NodeIO(KafkaSerializers)
    private val initService = InitService()
    private val offset = AtomicInteger()
    private val log = HashMap<String, MutableList<OffsetMessage>>()
    private val commits = HashMap<String, Int>()

    private fun nextOffset(): Int = offset.getAndIncrement()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    suspend fun serve() = withSingleThreadContext("requests") {
        serve(io) {
            request(initService::handle)
            request<Send> { message ->
                val offsetMessage = OffsetMessage(nextOffset(), message.body.msg)
                log.compute(message.body.key) { _, v ->
                    (v ?: ArrayList()).apply { add(offsetMessage) }
                }
                SendOk(message.body.msgId, offsetMessage.offset)
            }
            request<Poll> { message ->
                val offsetMessages = message.body.offsets
                    .mapValues { keyToOffset ->
                        log[keyToOffset.key]
                            ?.filter { it.offset >= keyToOffset.value }
                            ?: emptyList()
                    }
                PollOk(message.body.msgId, offsetMessages)
            }
            request<CommitOffsets> { message ->
                commits.putAll(message.body.offsets)
                CommitOffsetsOk(message.body.msgId)
            }
            request<ListCommittedOffsets> { message ->
                val offsets = message.body.keys
                    .filter { commits.contains(it) }
                    .associateWith { commits[it]!! }
                ListCommittedOffsetsOk(message.body.msgId, offsets)
            }
        }
    }
}

suspend fun main() = coroutineScope {
    val node = Node()
    node.serve()
}
