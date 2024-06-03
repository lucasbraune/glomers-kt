package io.github.lucasbraune.glomers.seqkv

import io.github.lucasbraune.glomers.protocol.Client
import io.github.lucasbraune.glomers.protocol.ResponseBody
import io.github.lucasbraune.glomers.protocol.RpcError
import io.github.lucasbraune.glomers.protocol.rpc
import io.github.lucasbraune.glomers.protocol.toException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Methods throw [io.github.lucasbraune.glomers.protocol.RpcException].
 */
class SeqKvStore<K, V>(
    private val client: Client,
    private val nextMsgId: () -> Int,
    private val keySerializer: SerializationStrategy<K>,
    private val valueSerializer: KSerializer<V>,
) {
    suspend fun read(key: K): V {
        val request = Read(nextMsgId(), encodeKey(key))
        val response = client.rpc<ResponseBody>(SEQ_KV_NODE_ID, request)
        when (response) {
            is ReadOk -> return decodeValue(response.value)
            is RpcError -> throw response.toException()
            else -> throw Exception("Bad response: $response")
        }
    }

    suspend fun write(key: K, value: V) {
        val request = Write(nextMsgId(), encodeKey(key), encodeValue(value))
        val response = client.rpc<ResponseBody>(SEQ_KV_NODE_ID, request)
        when (response) {
            is WriteOk -> return
            is RpcError -> throw response.toException()
            else -> throw Exception("Bad response: $response")
        }
    }

    suspend fun compareAndSet(key: K, from: V, to: V) {
        val request = CompareAndSet(nextMsgId(), encodeKey(key), encodeValue(from), encodeValue(to))
        val response = client.rpc<ResponseBody>(SEQ_KV_NODE_ID, request)
        when (response) {
            is CompareAndSetOk -> return
            is RpcError -> throw response.toException()
            else -> throw Exception("Bad response: $response")
        }
    }

    private fun encodeKey(key: K): String = Json.encodeToString(keySerializer, key)
    private fun encodeValue(value: V): String = Json.encodeToString(valueSerializer, value)
    private fun decodeValue(string: String): V = Json.decodeFromString(valueSerializer, string)

    private companion object {
        const val SEQ_KV_NODE_ID = "seq-kv"
    }
}

inline fun <reified K, reified V> SeqKvStore(
    client: Client,
    noinline nextMsgId: () -> Int,
) = SeqKvStore<K, V>(client, nextMsgId, serializer(), serializer())
