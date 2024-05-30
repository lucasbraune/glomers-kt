package io.github.lucasbraune.glomers.broadcast

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.modules.plus
import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.Log
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.rpc
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.util.RetryConfig
import io.github.lucasbraune.util.batches
import io.github.lucasbraune.util.retry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BroadcastNode {
    private val io = NodeIO(InitSerializersModule + BroadcastSerializersModule)
    private val initService = InitService()
    private val client = Client(io, initService)
    private val msgId = AtomicInteger()
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = ConcurrentHashMap.newKeySet<Int>()
    private val cachedMessages = AtomicReference<List<Int>>(emptyList())
    private val gossipQueue: Channel<Int> = Channel()

    private fun nextMsgId(): Int = msgId.getAndIncrement()

    suspend fun serve() {
        serve(io) {
            request(initService::handle)
            message(client::handle)
            request<Topology> {
                topology.complete(it.body.topology)
                TopologyOk(it.body.msgId)
            }
            request<Read> {
                ReadOk(cachedMessages.get(), it.body.msgId)
            }
            request<Broadcast> {
                val added = messages.add(it.body.message)
                if (added) {
                    cachedMessages.set(messages.toList())
                    gossipQueue.send(it.body.message)
                }
                BroadcastOk(it.body.msgId)
            }
            request<Gossip> {
//                Log.debug("Receiving gossip from ${it.src}: ${it.body.messages}")
                val newMessages = it.body.messages.minus(messages)
                val added = messages.addAll(it.body.messages)
                if (added) {
                    cachedMessages.set(messages.toList())
                    for (newMessage in newMessages) {
                        gossipQueue.send(newMessage)
                    }
                }
                GossipOk(it.body.msgId)
            }
        }
    }

    suspend fun work() = coroutineScope {
        val neighbors = topology.await()[initService.nodeId()]!!
        for (messageBatch in batches(gossipQueue, gossipDelay)) {
//            Log.debug("Sending gossip to $neighbors: $messageBatch")
            for (neighbor in neighbors) {
                launch {
                    retry(infiniteRetryConfig) {
                        val requestBody = Gossip(messageBatch, nextMsgId())
                        try {
                            withTimeout(rpcTimeout) {
                                client.rpc<GossipOk>(neighbor, requestBody)
                            }
                        } catch (ex: CancellationException) {
                            Log.error("Request to $neighbor timed out: $requestBody")
                            throw ex
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val gossipDelay = 300.milliseconds
        val rpcTimeout = 300.milliseconds
        val infiniteRetryConfig = RetryConfig(
            retries = Int.MAX_VALUE,
            backoff = 200.milliseconds,
            backoffMultiplier = 2.0,
            maxBackoff = 1.seconds,
        )
    }
}
