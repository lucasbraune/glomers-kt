package glomers.app

import glomers.protocol.Generate
import glomers.protocol.GenerateOk
import glomers.protocol.Init
import glomers.protocol.InitOk
import glomers.protocol.Message
import glomers.protocol.MessageHandler
import glomers.protocol.log
import glomers.protocol.send
import glomers.protocol.serve
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

data class ClusterInfo(val nodeId: String, val nodeIds: List<String>)

class UniqueIdHandler : MessageHandler {
    private val msgId = AtomicInteger()
    private val clusterInfo = CompletableDeferred<ClusterInfo>()
    private val generatedIdsCount = AtomicInteger()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun generateNextId(): Int {
        val (nodeId, nodeIds) = clusterInfo.getCompleted()
        val index = nodeIds.indexOf(nodeId)
        return generatedIdsCount.getAndIncrement() * nodeIds.size + index
    }

    override suspend fun invoke(it: Message) {
        when (it.body) {
            is Init -> {
                val (nodeId, nodeIds, msgId) = it.body
                clusterInfo.complete(ClusterInfo(nodeId, nodeIds))
                send(
                    Message(
                        src = it.dest,
                        dest = it.src,
                        body = InitOk(inReplyTo = msgId)
                    )
                )
            }

            is Generate -> {
                send(
                    Message(
                        src = it.dest,
                        dest = it.src,
                        body = GenerateOk(
                            msgId = msgId.getAndIncrement(),
                            inReplyTo = it.body.msgId,
                            id = generateNextId()
                        )
                    )
                )
            }

            else -> {
                log("Unexpected message: $it")
            }
        }
    }

}

fun serveUniqueId() = runBlocking {
    serve(handler = UniqueIdHandler())
}
