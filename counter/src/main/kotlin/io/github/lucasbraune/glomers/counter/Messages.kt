package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.Init
import io.github.lucasbraune.glomers.protocol.InitOk
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody
import io.github.lucasbraune.glomers.seqkv.SeqKvRequestSerializers
import io.github.lucasbraune.glomers.seqkv.SeqKvResponseSerializers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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

private val ServerRequestSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Init::class)
        subclass(Add::class)
        subclass(Read::class)
    }
}

private val ServerResponseSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(InitOk::class)
        subclass(AddOk::class)
        subclass(ReadOk::class)
    }
}

val CounterInputSerializers = ServerRequestSerializers + SeqKvResponseSerializers
val CounterOutputSerializers = ServerResponseSerializers + SeqKvRequestSerializers
