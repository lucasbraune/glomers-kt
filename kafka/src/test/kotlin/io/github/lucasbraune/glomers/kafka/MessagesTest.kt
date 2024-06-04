package io.github.lucasbraune.glomers.kafka

import io.github.lucasbraune.glomers.protocol.MessageBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessagesTest {
    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(MessageBody::class) { subclass(PollOk::class) }
        }
        @OptIn(ExperimentalSerializationApi::class)
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Test
    fun `test poll_ok body serializes`() {
        val pollOk = PollOk(
            inReplyTo = 1,
            msgs = mapOf(
                "k1" to listOf(OffsetMessage(1000, 9), OffsetMessage(1001, 5), OffsetMessage(1002, 15)),
                "k2" to listOf(OffsetMessage(2000, 7), OffsetMessage(2001, 2)),
            )
        )

        val actual = json.encodeToJsonElement<MessageBody>(pollOk)

        val expectedAsString = """
            {
              "type": "poll_ok",
              "in_reply_to": 1,
              "msgs": {
                "k1": [[1000, 9], [1001, 5], [1002, 15]],
                "k2": [[2000, 7], [2001, 2]]
              }
            }
        """.trimIndent()
        assertEquals(Json.decodeFromString<JsonElement>(expectedAsString), actual)
    }

    @Test
    fun `test poll_ok deserializes correctly`() {
        val string = """
            {
              "type": "poll_ok",
              "in_reply_to": 1,
              "msgs": {
                "k1": [[1000, 9], [1001, 5], [1002, 15]],
                "k2": [[2000, 7], [2001, 2]]
              }
            }
        """.trimIndent()

        val actual = json.decodeFromString<MessageBody>(string)

        val expected = PollOk(
            inReplyTo = 1,
            msgs = mapOf(
                "k1" to listOf(OffsetMessage(1000, 9), OffsetMessage(1001, 5), OffsetMessage(1002, 15)),
                "k2" to listOf(OffsetMessage(2000, 7), OffsetMessage(2001, 2)),
            )
        )
        assertEquals(expected, actual)
    }
}
