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

@Serializable
@SerialName("generate")
data class Generate(
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("generate_ok")
data class GenerateOk(
    val id: Int,
    override val msgId: Int,
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("broadcast")
data class Broadcast(
    val message: Int,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("broadcast_ok")
data class BroadcastOk(
    override val msgId: Int,
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
    override val msgId: Int,
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
    override val msgId: Int,
    override val inReplyTo: Int,
) : ResponseBody

@Serializable
@SerialName("gossip")
data class Gossip(
    val messages: List<Int>,
    override val msgId: Int,
) : RequestBody

@Serializable
@SerialName("gossip_ok")
data class GossipOk(
    override val inReplyTo: Int,
) : ResponseBody

