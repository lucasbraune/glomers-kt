package io.github.lucasbraune.glomers.kafka

import io.github.lucasbraune.glomers.protocol.ErrorCodes
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.RpcError
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Node {
    private val io = NodeIO(KafkaSerializers)
    private val initService = InitService()
    private val offset = AtomicInteger()
    private val log = HashMap<String, MutableList<OffsetMessage>>()
    private val logMutex = Mutex()
    private val commits = HashMap<String, Int>()
    private val commitMutex = Mutex()

    private fun nextOffset(): Int = offset.getAndIncrement()

    suspend fun serve() = serve(io) {
        request(initService::handle)
        request<Send> {
            val offset = nextOffset()
            logMutex.withLock {
                log.compute(it.body.key) { _, v ->
                    (v ?: ArrayList()).apply { add(OffsetMessage(offset, it.body.msg)) }
                }
            }
            SendOk(it.body.msgId, offset)
        }
        request<Poll> { message ->
            try {
                val msgs = logMutex.withLock {
                    message.body.offsets.mapValues { entry ->
                        requireNotNull(log[entry.key]).filter { offsetMessage ->
                            offsetMessage.offset >= entry.value
                        }
                    }
                }
                PollOk(message.body.msgId, msgs)
            } catch (ex: IllegalArgumentException) {
                RpcError(message.body.msgId, ErrorCodes.PRECONDITION_FAILED, ex.message ?: "")
            }
        }
        request<CommitOffsets> { message ->
            commitMutex.withLock {
                commits.putAll(message.body.offsets)
            }
            CommitOffsetsOk(message.body.msgId)
        }
        request<ListCommittedOffsets> { message ->
            val offsets = commitMutex.withLock {
                commits.toMutableMap().minus(message.body.keys.toSet())
            }
            ListCommittedOffsetsOk(message.body.msgId, offsets)
        }
    }
}

suspend fun main() = coroutineScope {
    val node = Node()
    node.serve()
}
