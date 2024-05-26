package uniqueid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import protocol2.MessageBody
import protocol2.RequestBody
import protocol2.ResponseBody

@Serializable
@SerialName("generate")
data class Generate(
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("generate_ok")
data class GenerateOk(
    val id: Int,
    override val inReplyTo: Int,
) : ResponseBody


val UniqueIdsSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Generate::class)
        subclass(GenerateOk::class)
    }
}
