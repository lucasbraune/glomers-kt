package io.github.lucasbraune.glomers.broadcast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody

@Serializable
@SerialName("broadcast")
data class Broadcast(
    val message: Int,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("broadcast_ok")
data class BroadcastOk(
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
    val messages: List<Int>,
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("topology")
data class Topology(
    val topology: Map<String, List<String>>,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("topology_ok")
data class TopologyOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("internal_broadcast")
data class InternalBroadcast(
    val messages: List<Int>,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("internal_broadcast_ok")
data class InternalBroadcastOk(
    override val inReplyTo: Int,
) : ResponseBody

val BroadcastSerializersModule = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Broadcast::class)
        subclass(BroadcastOk::class)
        subclass(Read::class)
        subclass(ReadOk::class)
        subclass(Topology::class)
        subclass(TopologyOk::class)
        subclass(InternalBroadcast::class)
        subclass(InternalBroadcastOk::class)
    }
}
