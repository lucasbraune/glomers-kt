package protocol2

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.cast

class Client(
    private val io: MessageIO,
    private val nodeId: Deferred<String>,
) {
    private val responsesByMsgId = ConcurrentHashMap<Int, CompletableDeferred<ResponseBody>>()

    suspend fun <Resp : ResponseBody> rpc(
        dest: String,
        request: RequestBody,
        clazz: KClass<Resp>,
    ): Resp {
        val response = CompletableDeferred<ResponseBody>()
        responsesByMsgId[request.msgId] = response
        try {
            io.send(Message(nodeId.await(), dest, request))
            return clazz.cast(response.await())
        } finally {
            responsesByMsgId.remove(request.msgId)
        }
    }

    fun handle(response: Message<ResponseBody>) {
        val deferred = responsesByMsgId.remove(response.body.inReplyTo)
        if (deferred != null) {
            deferred.complete(response.body)
        } else {
            Log.warning("Unexpected response: $response")
        }
    }
}

suspend inline fun <reified Resp : ResponseBody> Client.rpc(
    dest: String,
    request: RequestBody,
): Resp = rpc(dest, request, Resp::class)
