package glomers.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Serializable
sealed interface MessageBody

@Serializable
data class Message(
    val src: String,
    val dest: String,
    val body: MessageBody,
)

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

fun receiveMessage(): Message? =
    readlnOrNull()?.let { json.decodeFromString<Message>(it) }

fun send(message: Message) {
    println(json.encodeToString<Message>(message))
}

fun log(x: Any) {
    System.err.println(x)
}
