package glomers.broadcast

import chunk
import glomers.protocol.Broadcast
import glomers.protocol.BroadcastOk
import glomers.protocol.Client
import glomers.protocol.Gossip
import glomers.protocol.Init
import glomers.protocol.InitOk
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

data class ClusterInfo(val nodeId: String, val nodeIds: List<String>)

@OptIn(ExperimentalCoroutinesApi::class)
class BroadcastHandler : MessageHandler, AutoCloseable {
    private val nextMsgId = AtomicInteger()
    private val clusterInfo = CompletableDeferred<ClusterInfo>()
    private val client: Client by lazy { Client(clusterInfo.getCompleted().nodeId) }
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()
    private val leaderId: String by lazy { clusterInfo.getCompleted().nodeIds.first() }
    private val isLeader: Boolean by lazy {
        val (nodeId, nodeIds) = clusterInfo.getCompleted()
        nodeId == nodeIds.first()
    }
    private val channel by lazy { Channel<Int>() }
    private val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
    private val chunkedChannel: ReceiveChannel<List<Int>> by lazy {
        scope.chunk(channel, 500)
    }

    override fun close() {
        scope.cancel()
    }

    suspend fun broadcastToFollowers() = coroutineScope {
        val followerIds = clusterInfo.getCompleted().nodeIds.drop(1)
        for (chunk in chunkedChannel) {
            log("Leader broadcast: $chunk")
            for (followerId in followerIds) {
                launch {
                    client.rpc(followerId, Gossip(chunk, nextMsgId.getAndIncrement()))
                }
            }
        }
    }

    override suspend fun invoke(message: Message) {
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
                messages.add(message.body.message)
                reply(
                    message,
                    BroadcastOk(
                        msgId = nextMsgId.getAndIncrement(),
                        inReplyTo = message.body.msgId
                    )
                )
                if (!isLeader && message.src != leaderId) {
                    client.rpc(
                        leaderId,
                        Broadcast(message.body.message, nextMsgId.getAndIncrement())
                    )
                }
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

            else -> log("Unexpected message: $message")
        }
    }
}

fun serveBroadcast() = runBlocking(Dispatchers.Default) {
    BroadcastHandler().use {
        launch {
            serve(handler = it)
        }
        launch {
            it.broadcastToFollowers()
        }
    }
}
