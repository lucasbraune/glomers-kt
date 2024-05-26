package protocol2

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.*

@Serializable
@SerialName("init")
data class Init(
    val nodeId: String,
    val nodeIds: List<String>,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("init_ok")
data class InitOk(
    override val inReplyTo: Int,
) : ResponseBody

val InitSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Init::class)
        subclass(InitOk::class)
    }
}

class InitService {
    private val nodeId = CompletableDeferred<String>()
    private val nodeIds = CompletableDeferred<List<String>>()
    suspend fun nodeId(): String = nodeId.await()
    suspend fun nodeIds(): List<String> = nodeIds.await()
    fun handle(message: Message<Init>): InitOk {
        nodeId.complete(message.body.nodeId)
        nodeIds.complete(message.body.nodeIds)
        return InitOk(message.body.msgId)
    }
}
