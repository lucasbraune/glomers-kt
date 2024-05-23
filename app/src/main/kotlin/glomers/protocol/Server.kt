package glomers.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun interface MessageHandler {
    suspend fun handle(message: Message)
}

suspend fun serve(
    context: CoroutineContext = EmptyCoroutineContext,
    handler: MessageHandler
) {
    withContext(Dispatchers.IO) {
        while (true) {
            val message = receiveMessage() ?: break
            launch(Dispatchers.Default + context) {
                try {
                    handler.handle(message)
                } catch (ex: Exception) {
                    log(ex)
                }
            }
        }
    }
}
