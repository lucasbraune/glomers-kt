package protocol2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

class MessageIO(
    serializersModule: SerializersModule,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        this.serializersModule = serializersModule
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun send(message: Message<MessageBody>) {
        println(json.encodeToString<Message<MessageBody>>(message))
    }

    internal suspend fun receive(): Message<MessageBody>? = withContext(Dispatchers.IO) {
        readlnOrNull()?.let { json.decodeFromString<Message<MessageBody>>(it) }
    }
}

internal suspend fun serveBasic(
    io: MessageIO,
    handle: suspend (message: Message<MessageBody>) -> Unit,
) = coroutineScope {
    while (true) {
        val message = io.receive() ?: break
        launch {
            try {
                handle.invoke(message)
            } catch (ex: Throwable) {
                Log.error("Unable to handle $message. Details: ${ex.stackTraceToString()}")
            }
        }
    }
}
