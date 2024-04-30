package glomers.protocol

sealed interface RpcMessageBody : MessageBody {
    val msgId: Int?
        get() = null
    val inReplyTo: Int?
        get() = null
}

sealed interface RequestBody : RpcMessageBody {
    override val msgId: Int
}

sealed interface ResponseBody : RpcMessageBody {
    override val inReplyTo: Int
}
