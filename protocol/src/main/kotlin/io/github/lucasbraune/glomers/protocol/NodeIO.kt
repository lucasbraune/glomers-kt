package io.github.lucasbraune.glomers.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

// TODO: Review changes introduced with this comment.
class NodeIO(
    inputSerializers: SerializersModule,
    outputSerializers: SerializersModule,
) {
    constructor(serializersModule: SerializersModule) : this(serializersModule, serializersModule)

    private val inputJson = json(inputSerializers)
    private val outputJson = json(outputSerializers)

    private fun json(serializersModule: SerializersModule) = Json {
        this.serializersModule = serializersModule
        ignoreUnknownKeys = true
        @OptIn(ExperimentalSerializationApi::class)
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun send(message: Message<out MessageBody>) {
        println(outputJson.encodeToString<Message<out MessageBody>>(message))
    }

    internal suspend fun receive(): Message<out MessageBody>? = withContext(Dispatchers.IO) {
        readlnOrNull()?.let { inputJson.decodeFromString<Message<out MessageBody>>(it) }
    }
}
