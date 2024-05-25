package glomers.broadcast

import chunk
import glomers.util.RetryOptions
import glomers.util.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import protocol2.Client
import protocol2.InitSerializersModule
import protocol2.InitService
import protocol2.Log
import protocol2.Message
import protocol2.MessageBody
import protocol2.MessageIO
import protocol2.RequestBody
import protocol2.ResponseBody
import protocol2.message
import protocol2.request
import protocol2.rpc
import protocol2.serveRoutes
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

class BroadcastNode {
    private val io = MessageIO(InitSerializersModule + BroadcastSerializersModule)
    private val initService = InitService()
    private val client = Client(io, initService.nodeId)
    private val msgId = AtomicInteger()
    private val topology = CompletableDeferred<Map<String, List<String>>>()
    private val messages = CopyOnWriteArrayList<Int>()
    private val internalBroadcastChannel: Channel<Int> = Channel()

    private suspend fun nodeId(): String = initService.nodeId.await()
    private suspend fun nodeIds(): List<String> = initService.nodeIds.await()
    private suspend fun leaderId(): String = nodeIds().first()

    suspend fun work() = coroutineScope {
        val otherNodeIds = nodeIds().filter { it != nodeId() }
        val messageChunks = chunk(internalBroadcastChannel, delayBetweenBroadcasts)
        for (messageChunk in messageChunks) {
            Log.info("Internal broadcast: $messageChunk")
            for (otherNodeId in otherNodeIds) {
                launch {
                    retry(infiniteRetries) {
                        withTimeout(rpcTimeout) {
                            client.rpc<InternalBroadcastOk>(
                                otherNodeId,
                                InternalBroadcast(messageChunk, msgId.getAndIncrement())
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun serve() {
        serveRoutes(io) {
            request(initService::handle)
            message(client::handle)
            message<Broadcast> {
                Log.info("Received: ${it.body.message}")
                val added = messages.addIfAbsent(it.body.message)
                io.send(Message(it.dest, it.src,
                    BroadcastOk(msgId.getAndIncrement(), it.body.msgId)))
                if (added) {
                    if (nodeId() == leaderId()) {
                        internalBroadcastChannel.send(it.body.message)
                    } else {
                        try {
                            withTimeout(rpcTimeout) {
                                client.rpc<BroadcastOk>(leaderId(),
                                    Broadcast(it.body.message, msgId.getAndIncrement()))
                            }
                        } catch (ex: Exception) {
                            Log.warning("Unable to reach leader; broadcasting ${it.body.message}")
                            internalBroadcastChannel.send(it.body.message)
                        }
                    }
                }
            }
            request<InternalBroadcast> {
                messages.addAllAbsent(it.body.messages)
                InternalBroadcastOk(it.body.msgId)
            }
            request<Read> {
                ReadOk(messages, msgId.getAndIncrement(), it.body.msgId)
            }
            request<Topology> {
                topology.complete(it.body.topology)
                TopologyOk(msgId.getAndIncrement(), it.body.msgId)
            }
        }
    }

    private companion object {
        val delayBetweenBroadcasts: Duration = 300.milliseconds
        val rpcTimeout = 300.milliseconds
        val infiniteRetries = RetryOptions(
            retries = Int.MAX_VALUE,
            backoff = 200.milliseconds,
            backoffMultiplier = 2.0,
            maxBackoff = 1.seconds,
        )
    }
}

fun serveBroadcast() = runBlocking(Dispatchers.Default) {
    val node = BroadcastNode()
    launch { node.work() }
    node.serve()
}
