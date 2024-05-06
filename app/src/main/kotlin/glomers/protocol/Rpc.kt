package glomers.protocol

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

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

fun reply(request: Message, responseBody: ResponseBody) {
    send(
        Message(
            src = request.dest,
            dest = request.src,
            body = responseBody,
        )
    )
}

class Client(
    private val nodeId: String,
    private val defaultTimeout: Duration? = null,
) {
    private val outstandingResponses = ConcurrentHashMap<Int, CompletableDeferred<ResponseBody>>()

    private fun send(dest: String, body: MessageBody) = send(Message(src = nodeId, dest, body))

    suspend fun rpc(
        dest: String,
        request: RequestBody,
        timeout: Duration? = defaultTimeout,
    ): ResponseBody = if (timeout == null) {
        doRpc(dest, request)
    } else {
        withTimeout(timeout) { doRpc(dest, request) }
    }

    private suspend fun doRpc(dest: String, request: RequestBody): ResponseBody {
        send(dest, request)
        val response = CompletableDeferred<ResponseBody>()
        outstandingResponses[request.msgId] = response
        try {
            return response.await()
        } catch (ex: CancellationException) {
            outstandingResponses.remove(request.msgId)
            throw ex
        }
    }

    fun handle(response: ResponseBody) {
        val outstandingResponse = outstandingResponses.remove(response.inReplyTo)
        if (outstandingResponse != null) {
            outstandingResponse.complete(response)
        } else {
            log("Unexpected response: $response")
        }
    }
}
