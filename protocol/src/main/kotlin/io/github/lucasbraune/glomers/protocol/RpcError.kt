package io.github.lucasbraune.glomers.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("error")
data class RpcError(
    override val inReplyTo: Int,
    val code: Int,
    val text: String,
): ResponseBody

class RpcException(
    val code: Int,
    message: String
): Exception("RPC error $code: $message")

fun RpcError.toException() = RpcException(code, text)

/**
 * Semantics: https://github.com/jepsen-io/maelstrom/blob/main/doc/protocol.md#errors
 */
object ErrorCodes {
    const val TIMEOUT = 0
    const val NODE_NOT_FOUND = 1
    const val NOT_SUPPORTED = 10
    const val TEMPORARILY_UNAVAILABLE = 11
    const val MALFORMED_REQUEST = 12
    const val CRASH = 13
    const val ABORT = 14
    const val KEY_DOES_NOT_EXIST = 20
    const val KEY_ALREADY_EXISTS = 21
    const val PRECONDITION_FAILED = 22
    const val TRANSACTION_CONFLICT = 30
}
