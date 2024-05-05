package glomers.protocol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.invoke.MethodHandles.loop
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.coroutineScope as coroutineScope1

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
