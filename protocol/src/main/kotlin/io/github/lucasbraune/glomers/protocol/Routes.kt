package io.github.lucasbraune.glomers.protocol

import kotlin.reflect.KClass

interface RouterBuilder {
    fun message(
        predicate: (message: Message<out MessageBody>) -> Boolean,
        handle: suspend (message: Message<out MessageBody>) -> Unit,
    )

    fun <Req : RequestBody> request(
        clazz: KClass<Req>,
        handle: suspend (Message<Req>) -> ResponseBody,
    )
}

inline fun <reified Body : MessageBody> RouterBuilder.message(
    noinline handle: suspend (Message<Body>) -> Unit
) {
    message({ it.body is Body }) {
        @Suppress("UNCHECKED_CAST")
        handle(it as Message<Body>)
    }
}

inline fun <reified Req : RequestBody> RouterBuilder.request(
    noinline handle: suspend (Message<Req>) -> ResponseBody
) {
    request(Req::class, handle)
}

internal class Router(
    private val io: NodeIO
) : RouterBuilder {
    private data class Route(
        val predicate: (message: Message<out MessageBody>) -> Boolean,
        val handler: suspend (message: Message<out MessageBody>) -> Unit,
    )

    private val routes = mutableListOf<Route>()

    override fun message(
        predicate: (message: Message<out MessageBody>) -> Boolean,
        handle: suspend (message: Message<out MessageBody>) -> Unit,
    ) {
        routes.add(Route(predicate, handle))
    }

    override fun <Req : RequestBody> request(
        clazz: KClass<Req>,
        handle: suspend (Message<Req>) -> ResponseBody,
    ) {
        message({ clazz.isInstance(it.body) }) {
            @Suppress("UNCHECKED_CAST")
            io.send(Message(it.dest, it.src, handle(it as Message<Req>)))
        }
    }

    suspend fun handle(message: Message<out MessageBody>) {
        routes.firstOrNull { it.predicate(message) }
            ?.handler?.invoke(message)
            ?: Log.warning("No handler registered for $message")
    }
}
