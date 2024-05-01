package glomers.broadcast

import glomers.protocol.Broadcast
import glomers.protocol.BroadcastOk
import glomers.protocol.Init
import glomers.protocol.InitOk
import glomers.protocol.Message
import glomers.protocol.MessageBody
import glomers.protocol.MessageHandler
import glomers.protocol.Read
import glomers.protocol.ReadOk
import glomers.protocol.ResponseBody
import glomers.protocol.Topology
import glomers.protocol.TopologyOk
import glomers.protocol.log
import glomers.protocol.send
import glomers.protocol.serve
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

fun reply(request: Message, responseBody: MessageBody) {
    send(
        Message(
            src = request.dest,
            dest = request.src,
            body = responseBody,
        )
    )
}

data class ClusterInfo(val nodeId: String, val nodeIds: List<String>)

class BroadcastHandler : MessageHandler {
    private val msgId = AtomicInteger()
    private val clusterInfo = CompletableDeferred<ClusterInfo>()
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()

    override suspend fun invoke(message: Message) {
        when (message.body) {
            is Init -> {
                val (nodeId, nodeIds, msgId) = message.body
                clusterInfo.complete(ClusterInfo(nodeId, nodeIds))
                reply(message, InitOk(inReplyTo = msgId))
            }

            is ResponseBody -> TODO()

            is Broadcast -> {
                messages.add(message.body.message)
                reply(
                    message, BroadcastOk(
                        msgId = msgId.getAndIncrement(),
                        inReplyTo = message.body.msgId,
                    )
                )
            }

            is Read -> {
                reply(
                    message, ReadOk(
                        messages = messages,
                        msgId = msgId.getAndIncrement(),
                        inReplyTo = message.body.msgId,
                    )
                )
            }

            is Topology -> {
                topology.complete(message.body.topology)
                reply(
                    message, TopologyOk(
                        msgId = msgId.getAndIncrement(),
                        inReplyTo = message.body.msgId
                    )
                )
            }

            else -> log("Unexpected message: $message")
        }
    }
}

fun serveBroadcast() = runBlocking {
    serve(handler = BroadcastHandler())
}
