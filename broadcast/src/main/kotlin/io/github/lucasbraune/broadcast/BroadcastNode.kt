package io.github.lucasbraune.broadcast

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.modules.plus
import io.github.lucasbraune.protocol.Client
import io.github.lucasbraune.protocol.InitSerializersModule
import io.github.lucasbraune.protocol.InitService
import io.github.lucasbraune.protocol.Log
import io.github.lucasbraune.protocol.Message
import io.github.lucasbraune.protocol.MessageIO
import io.github.lucasbraune.protocol.message
import io.github.lucasbraune.protocol.request
import io.github.lucasbraune.protocol.rpc
import io.github.lucasbraune.protocol.serveRoutes
import io.github.lucasbraune.util.RetryOptions
import io.github.lucasbraune.util.chunk
import io.github.lucasbraune.util.retry
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BroadcastNode {
    private val io = MessageIO(InitSerializersModule + BroadcastSerializersModule)
    private val initService = InitService()
    private val client = Client(io, initService::nodeId)
    private val msgId = AtomicInteger()
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()
    private val internalBroadcastChannel: Channel<Int> = Channel()

    private suspend fun leaderId(): String = initService.nodeIds().first()

    suspend fun work() = coroutineScope {
        val otherNodeIds = with(initService) {
            nodeIds().filter { it != nodeId() }
        }
        val messageChunks = chunk(internalBroadcastChannel, delayBetweenBroadcasts)
        for (messageChunk in messageChunks) {
            Log.info("Internal broadcast: $messageChunk")
            for (otherNodeId in otherNodeIds) {
                launch {
                    retry(infiniteRetries) {
                        withTimeout(rpcTimeout) {
                            client.rpc<InternalBroadcastOk>(
                                otherNodeId,
                                InternalBroadcast(messageChunk, msgId.getAndIncrement())
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun serve() {
        serveRoutes(io) {
            request(initService::handle)
            message(client::handle)
            message<Broadcast> {
                Log.info("Received: ${it.body.message}")
                val added = messages.addIfAbsent(it.body.message)
                io.send(Message(it.dest, it.src, BroadcastOk(it.body.msgId)))
                if (added) {
                    if (initService.nodeId() == leaderId()) {
                        internalBroadcastChannel.send(it.body.message)
                    } else {
                        try {
                            withTimeout(rpcTimeout) {
                                client.rpc<BroadcastOk>(
                                    leaderId(),
                                    Broadcast(it.body.message, msgId.getAndIncrement())
                                )
                            }
                        } catch (ex: Exception) {
                            Log.warning("Unable to reach leader; broadcasting ${it.body.message}")
                            internalBroadcastChannel.send(it.body.message)
                        }
                    }
                }
            }
            request<InternalBroadcast> {
                messages.addAllAbsent(it.body.messages)
                InternalBroadcastOk(it.body.msgId)
            }
            request<Read> { ReadOk(messages, it.body.msgId) }
            request<Topology> {
                topology.complete(it.body.topology)
                TopologyOk(it.body.msgId)
            }
        }
    }

    private companion object {
        val delayBetweenBroadcasts: Duration = 300.milliseconds
        val rpcTimeout = 300.milliseconds
        val infiniteRetries = RetryOptions(
            retries = Int.MAX_VALUE,
            backoff = 200.milliseconds,
            backoffMultiplier = 2.0,
            maxBackoff = 1.seconds,
        )
    }
}
