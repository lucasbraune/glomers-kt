package glomers.broadcast

import chunk
import glomers.protocol.Broadcast
import glomers.protocol.BroadcastOk
import glomers.protocol.Client
import glomers.protocol.Echo
import glomers.protocol.Generate
import glomers.protocol.InternalBroadcast
import glomers.protocol.Init
import glomers.protocol.InitOk
import glomers.protocol.InternalBroadcastOk
import glomers.protocol.Message
import glomers.protocol.MessageHandler
import glomers.protocol.Read
import glomers.protocol.ReadOk
import glomers.protocol.ResponseBody
import glomers.protocol.Topology
import glomers.protocol.TopologyOk
import glomers.protocol.log
import glomers.protocol.reply
import glomers.protocol.serve
import glomers.util.RetryOptions
import glomers.util.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class ClusterInfo(val nodeId: String, val nodeIds: List<String>)

@OptIn(ExperimentalCoroutinesApi::class)
class BroadcastNode : MessageHandler {
    private val nextMsgId = AtomicInteger()

    private val clusterInfo = CompletableDeferred<ClusterInfo>()
    private val nodeId: String by lazy { clusterInfo.getCompleted().nodeId }
    private val client: Client by lazy {
        Client(clusterInfo.getCompleted().nodeId, rpcTimeout)
    }

    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()

    private val leaderId: String by lazy { clusterInfo.getCompleted().nodeIds.first() }

    private val channel: Channel<Int> = Channel()

    suspend fun work() = coroutineScope {
        val (nodeId, nodeIds) = clusterInfo.await()
        val otherNodeIds = nodeIds.filter { it != nodeId }
        val messageChunks = chunk(channel, delayBetweenBroadcasts)
        for (messageChunk in messageChunks) {
            log("Internal broadcast: $messageChunk")
            for (otherNodeId in otherNodeIds) {
                launch {
                    retry(infiniteRetries) {
                        client.rpc(
                            otherNodeId,
                            InternalBroadcast(messageChunk, nextMsgId.getAndIncrement())
                        )
                    }
                }
            }
        }
    }

    suspend fun serve() = serve(handler = this)

    override suspend fun handle(message: Message) {
        when (message.body) {
            is Init -> {
                val (nodeId, nodeIds, msgId) = message.body
                clusterInfo.complete(ClusterInfo(nodeId, nodeIds))
                reply(message, InitOk(inReplyTo = msgId))
            }

            is ResponseBody -> {
                client.handle(message.body)
            }

            is Broadcast -> {
                log("$nodeId received: ${message.body.message}")
                val added = messages.addIfAbsent(message.body.message)
                reply(
                    message,
                    BroadcastOk(
                        msgId = nextMsgId.getAndIncrement(),
                        inReplyTo = message.body.msgId
                    )
                )
                if (added) {
                    if (nodeId == leaderId) {
                        channel.send(message.body.message)
                    } else {
                        try {
                            client.rpc(
                                leaderId,
                                Broadcast(message.body.message, nextMsgId.getAndIncrement())
                            )
                        } catch (ex: Exception) {
                            log("Unable to reach leader; broadcasting ${message.body.message}")
                            channel.send(message.body.message)
                        }
                    }
                }
            }

            is InternalBroadcast -> {
                messages.addAllAbsent(message.body.messages)
                reply(message, InternalBroadcastOk(message.body.msgId))
            }

            is Read -> {
                reply(
                    message,
                    ReadOk(
                        messages = messages,
                        msgId = nextMsgId.getAndIncrement(),
                        inReplyTo = message.body.msgId,
                    )
                )
            }

            is Topology -> {
                topology.complete(message.body.topology)
                reply(
                    message, TopologyOk(
                        msgId = nextMsgId.getAndIncrement(),
                        inReplyTo = message.body.msgId
                    )
                )
            }

            is Echo, is Generate -> log("Unexpected message: $message")
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

fun serveBroadcast() = runBlocking(Dispatchers.Default) {
    val node = BroadcastNode()
    launch { node.serve() }
    launch { node.work() }
}
