package io.github.lucasbraune.glomers.protocol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.receiveMessages(io: NodeIO) = produce(Dispatchers.IO) {
    while (true) {
        val message = io.receive() ?: break
        send(message)
    }
}

suspend fun serve(
    io: NodeIO,
    builderAction: RouterBuilder.() -> Unit,
) {
    val router = Router(io).apply { builderAction() }
    coroutineScope {
        val messageChannel = receiveMessages(io)
        for (message in messageChannel) {
            launch {
                try {
                    router.handle(message)
                } catch (ex: Throwable) {
                    Log.error("Unable to handle $message. Details: ${ex.stackTraceToString()}")
                }
            }
        }
    }
}
