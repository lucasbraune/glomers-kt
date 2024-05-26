package echo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import protocol2.MessageBody
import protocol2.RequestBody
import protocol2.ResponseBody

@Serializable
@SerialName("echo")
data class Echo(
    val echo: String,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("echo_ok")
data class EchoOk(
    val echo: String,
    override val inReplyTo: Int,
) : ResponseBody

val EchoSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Echo::class)
        subclass(EchoOk::class)
    }
}
