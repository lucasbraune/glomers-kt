package io.github.lucasbraune.glomers.services.seqkv

import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("read")
data class Read(
    override val msgId: Int,
    val key: String,
) : RequestBody

@Serializable
@SerialName("read_ok")
data class ReadOk(
    override val inReplyTo: Int,
    val value: Int,
) : ResponseBody

@Serializable
@SerialName("write")
data class Write(
    override val msgId: Int,
    val key: String,
    val value: Int,
) : RequestBody

@Serializable
@SerialName("write_ok")
data class WriteOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("cas")
data class CompareAndSet(
    override val msgId: Int,
    val key: String,
    val from: Int,
    val to: Int,
) : RequestBody

@Serializable
@SerialName("cas_ok")
data class CompareAndSetOk(
    override val inReplyTo: Int,
) : ResponseBody

val CounterSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Read::class)
        subclass(ReadOk::class)
        subclass(Write::class)
        subclass(WriteOk::class)
        subclass(CompareAndSet::class)
        subclass(CompareAndSetOk::class)
    }
}

const val SEQ_KV_NODE_ID = "seq-kv"