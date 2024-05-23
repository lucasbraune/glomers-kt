package protocol2

import kotlin.reflect.KClass

private typealias Handler = suspend (message: Message<MessageBody>) -> Unit

class HandlerBuilder internal constructor(
    private val io: MessageIO,
) {
    private data class Route(
        val predicate: (message: Message<MessageBody>) -> Boolean,
        val handler: suspend (message: Message<MessageBody>) -> Unit,
    )

    private val routes = mutableListOf<Route>()

    fun message(
        predicate: (message: Message<MessageBody>) -> Boolean,
        handle: suspend (message: Message<MessageBody>) -> Unit,
    ) {
        routes.add(Route(predicate, handle))
    }

    fun <Req : RequestBody> request(
        clazz: KClass<Req>,
        handle: suspend (Message<Req>) -> ResponseBody,
    ) {
        message({ clazz.isInstance(it.body) }) {
            @Suppress("UNCHECKED_CAST")
            io.send(Message(it.dest, it.src, handle(it as Message<Req>)))
        }
    }

    fun build(): Handler {
        val routesCopy = routes.toList()
        return { message ->
            routesCopy.firstOrNull { it.predicate(message) }
                ?.handler?.invoke(message)
                ?: Log.warning("No handler registered for $message")
        }
    }
}

inline fun <reified Body : MessageBody> HandlerBuilder.message(
    noinline handle: suspend (Message<Body>) -> Unit
) {
    message({ it.body is Body }) {
        @Suppress("UNCHECKED_CAST")
        handle(it as Message<Body>)
    }
}

inline fun <reified Req : RequestBody> HandlerBuilder.request(
    noinline handle: suspend (Message<Req>) -> ResponseBody
) {
    request(Req::class, handle)
}

suspend fun serve(
    io: MessageIO,
    builderAction: HandlerBuilder.() -> Unit = {}
) {
    serveBasic(io, HandlerBuilder(io).apply { builderAction() }.build())
}
