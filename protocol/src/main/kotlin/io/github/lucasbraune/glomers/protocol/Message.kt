package io.github.lucasbraune.glomers.protocol

import kotlinx.serialization.Serializable

interface MessageBody

@Serializable
data class Message<Body : MessageBody>(
    val src: String,
    val dest: String,
    val body: Body
)

interface RpcMessageBody : MessageBody {
    val msgId: Int?
        get() = null
    val inReplyTo: Int?
        get() = null
}

interface RequestBody : RpcMessageBody {
    override val msgId: Int
}

interface ResponseBody : RpcMessageBody {
    override val inReplyTo: Int
}
