package glomers.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("init")
data class Init(
    val nodeId: String,
    val nodeIds: List<String>,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("init_ok")
data class InitOk(
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("echo")
data class Echo(
    val echo: String,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("echo_ok")
data class EchoOk(
    val echo: String,
    override val msgId: Int,
    override val inReplyTo: Int,
) : ResponseBody
