package protocol2

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
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
    private val _nodeId = CompletableDeferred<String>()
    private val _nodeIds = CompletableDeferred<List<String>>()
    val nodeId: Deferred<String> = _nodeId
    val nodeIds: Deferred<List<String>> = _nodeIds
    fun handle(message: Message<Init>): InitOk {
        _nodeId.complete(message.body.nodeId)
        _nodeIds.complete(message.body.nodeIds)
        return InitOk(message.body.msgId)
    }
}
