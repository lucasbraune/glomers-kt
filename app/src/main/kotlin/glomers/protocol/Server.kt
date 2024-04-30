package glomers.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias MessageHandler = suspend (message: Message) -> Unit

suspend fun serve(
    context: CoroutineContext = EmptyCoroutineContext,
    handler: MessageHandler
) {
    withContext(Dispatchers.IO) {
        while (true) {
            val message = receiveMessage() ?: break
            launch(Dispatchers.Default + context) {
                try {
                    handler(message)
                } catch (ex: Exception) {
                    log(ex)
                }
            }
        }
    }
}
