package io.github.lucasbraune.glomers.protocol

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun serve(
    io: NodeIO,
    builderAction: RouterBuilder.() -> Unit,
) {
    coroutineScope {
        val router = Router(io).apply { builderAction() }
        while (true) {
            val message = io.receive() ?: break
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
