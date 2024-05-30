package io.github.lucasbraune.glomers.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

class NodeIO(
    serializersModule: SerializersModule,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        this.serializersModule = serializersModule
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun send(message: Message<out MessageBody>) {
        println(json.encodeToString<Message<out MessageBody>>(message))
    }

    internal suspend fun receive(): Message<out MessageBody>? = withContext(Dispatchers.IO) {
        readlnOrNull()?.let { json.decodeFromString<Message<out MessageBody>>(it) }
    }
}