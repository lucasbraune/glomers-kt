package io.github.lucasbraune.glomers.seqkv

import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody
import io.github.lucasbraune.glomers.protocol.RpcError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("read")
internal data class Read(
    override val msgId: Int,
    val key: String,
) : RequestBody

@Serializable
@SerialName("read_ok")
internal data class ReadOk(
    override val inReplyTo: Int,
    val value: String,
) : ResponseBody

@Serializable
@SerialName("write")
internal data class Write(
    override val msgId: Int,
    val key: String,
    val value: String,
) : RequestBody

@Serializable
@SerialName("write_ok")
internal data class WriteOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("cas")
internal data class CompareAndSet(
    override val msgId: Int,
    val key: String,
    val from: String,
    val to: String,
) : RequestBody

@Serializable
@SerialName("cas_ok")
internal data class CompareAndSetOk(
    override val inReplyTo: Int,
) : ResponseBody

val SeqKvRequestSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Read::class)
        subclass(Write::class)
        subclass(CompareAndSet::class)
    }
}

val SeqKvResponseSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(ReadOk::class)
        subclass(WriteOk::class)
        subclass(CompareAndSetOk::class)
        subclass(RpcError::class)
    }
}
