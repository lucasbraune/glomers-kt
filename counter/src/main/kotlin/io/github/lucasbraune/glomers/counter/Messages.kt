package io.github.lucasbraune.glomers.counter

import io.github.lucasbraune.glomers.protocol.Init
import io.github.lucasbraune.glomers.protocol.InitOk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody
import io.github.lucasbraune.glomers.protocol.RpcError
import io.github.lucasbraune.glomers.services.seqkv.CompareAndSet
import io.github.lucasbraune.glomers.services.seqkv.CompareAndSetOk
import io.github.lucasbraune.glomers.services.seqkv.Write
import io.github.lucasbraune.glomers.services.seqkv.WriteOk

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

val CounterInputSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        // Init request
        subclass(Init::class)

        // Counter requests
        subclass(Add::class)
        subclass(Read::class)

        // SeqKv responses
        subclass(io.github.lucasbraune.glomers.services.seqkv.ReadOk::class)
        subclass(WriteOk::class)
        subclass(CompareAndSetOk::class)
        subclass(RpcError::class)
    }
}

val CounterOutputSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        // Init response
        subclass(InitOk::class)

        // Counter responses
        subclass(AddOk::class)
        subclass(ReadOk::class)

        // SeqKv requests
        subclass(io.github.lucasbraune.glomers.services.seqkv.Read::class)
        subclass(Write::class)
        subclass(CompareAndSet::class)
    }
}
