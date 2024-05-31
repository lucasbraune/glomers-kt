package io.github.lucasbraune.glomers.broadcast

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.Log
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.message
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.rpc
import io.github.lucasbraune.glomers.protocol.serve
import io.github.lucasbraune.util.RetryOptions
import io.github.lucasbraune.util.chunk
import io.github.lucasbraune.util.retry
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.modules.plus

class BroadcastNode {
    private val io = NodeIO(InitSerializersModule + BroadcastSerializersModule)
    private val initService = InitService()
    private val client = Client(io, initService)
    private val msgId = AtomicInteger()
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()
    private val gossipChannel: Channel<Int> = Channel()

    private suspend fun leaderId(): String {
        // Nodes use seconds on wall clock to agree on leader with high probability.
        // Disagreement hinders performance, but does not affect correctness.
        // Rotating leadership helps the system recover during network partitions.
        val seed = Instant.now().epochSecond
        val nodeIds = initService.nodeIds()
        return nodeIds[abs(hash(seed.toInt())) % nodeIds.size]
    }

    suspend fun serve() {
        serve(io) {
            request(initService::handle)
            message(client::handle)
            request<Topology> {
                topology.complete(it.body.topology)
                TopologyOk(it.body.msgId)
            }
            request<Read> {
                ReadOk(messages, it.body.msgId)
            }
            request<Broadcast> {
                val leaderId = leaderId()
                if (initService.nodeId() != leaderId) {
                    try {
                        val body = Broadcast(it.body.message, msgId.getAndIncrement())
                        withTimeout(rpcTimeout) { client.rpc<BroadcastOk>(leaderId, body) }
                    } catch (_: Throwable) {
                        Log.warning("Unable to forward request to leader ($leaderId).")
                        messages.addIfAbsent(it.body.message)
                        gossipChannel.send(it.body.message)
                    }
                } else {
                    messages.addIfAbsent(it.body.message)
                    gossipChannel.send(it.body.message)
                }
                BroadcastOk(it.body.msgId)
            }
            request<Gossip> {
                messages.addAllAbsent(it.body.messages)
                GossipOk(it.body.msgId)
            }
        }
    }

    suspend fun work() = coroutineScope {
        val nodeId = initService.nodeId()
        val otherNodeIds = initService.nodeIds().filter { it != nodeId }
        val messageChunks = chunk(gossipChannel, gossipPeriod)
        for (messageChunk in messageChunks) {
            Log.info("Gossiping: $messageChunk")
            for (otherNodeId in otherNodeIds) {
                launch {
                    retry(infiniteRetries) {
                        val gossipBody = Gossip(messageChunk, msgId.getAndIncrement())
                        withTimeout(rpcTimeout) { client.rpc<GossipOk>(otherNodeId, gossipBody) }
                    }
                }
            }
        }
    }

    private companion object {
        val gossipPeriod = 100.milliseconds
        val rpcTimeout = 300.milliseconds
        val infiniteRetries = RetryOptions(
            retries = Int.MAX_VALUE,
            backoff = 200.milliseconds,
            backoffMultiplier = 2.0,
            maxBackoff = 1.seconds,
        )

        fun hash(x: Int): Int {
            var h = x
            h = ((h ushr 16) xor h) * 0x45d9f3b;
            h = ((h ushr 16) xor h) * 0x45d9f3b;
            h = (h ushr 16) xor h;
            return h
        }
    }
}
