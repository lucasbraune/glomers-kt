package io.github.lucasbraune.glomers.counter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody

@Serializable
@SerialName("add")
data class Add(
    override val msgId: Int,
    val delta: Int,
) : RequestBody

@Serializable
@SerialName("add_ok")
data class AddOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("read")
data class Read(
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("read_ok")
data class ReadOk(
    override val inReplyTo: Int,
    val value: Int,
) : ResponseBody

val CounterSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Add::class)
        subclass(AddOk::class)
        subclass(Read::class)
        subclass(ReadOk::class)
    }
}
