package io.github.lucasbraune.glomers.kafka

import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.MessageBody
import io.github.lucasbraune.glomers.protocol.RequestBody
import io.github.lucasbraune.glomers.protocol.ResponseBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("send")
data class Send(
    override val msgId: Int,
    val key: String,
    val msg: Int,
) : RequestBody

@Serializable
@SerialName("send_ok")
data class SendOk(
    override val inReplyTo: Int,
    val offset: Int,
) : ResponseBody

@Serializable
@SerialName("poll")
data class Poll(
    override val msgId: Int,
    val offsets: Map<String, Int>,
) : RequestBody

@Serializable(with = OffsetMessageSerializer::class)
data class OffsetMessage(val offset: Int, val message: Int)

@Serializable
@SerialName("poll_ok")
data class PollOk(
    override val inReplyTo: Int,
    val msgs: Map<String, List<OffsetMessage>>
) : ResponseBody

@Serializable
@SerialName("commit_offsets")
data class CommitOffsets(
    override val msgId: Int,
    val offsets: Map<String, Int>,
) : RequestBody

@Serializable
@SerialName("commit_offsets_ok")
data class CommitOffsetsOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("list_committed_offsets")
data class ListCommittedOffsets(
    override val msgId: Int,
    val keys: List<String>,
) : RequestBody

@Serializable
@SerialName("list_committed_offsets_ok")
data class ListCommittedOffsetsOk(
    override val inReplyTo: Int,
    val offsets: Map<String, Int>,
) : ResponseBody

val KafkaSerializers = InitSerializersModule + SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Send::class)
        subclass(SendOk::class)
        subclass(Poll::class)
        subclass(PollOk::class)
        subclass(CommitOffsets::class)
        subclass(CommitOffsetsOk::class)
        subclass(ListCommittedOffsets::class)
        subclass(ListCommittedOffsetsOk::class)
    }
}

private class OffsetMessageSerializer : KSerializer<OffsetMessage> {
    private val delegate = ListSerializer(Int.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("OffsetMessage", delegate.descriptor)

    override fun deserialize(decoder: Decoder): OffsetMessage {
        val list = decoder.decodeSerializableValue(delegate)
        return OffsetMessage(list[0], list[1])
    }

    override fun serialize(encoder: Encoder, value: OffsetMessage) {
        val list = listOf(value.offset, value.message)
        encoder.encodeSerializableValue(delegate, list)
    }
}