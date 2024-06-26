package io.github.lucasbraune.glomers.uniqueid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody

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
