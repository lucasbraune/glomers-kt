package io.github.lucasbraune.glomers.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("error")
data class Error(
    override val inReplyTo: Int,
    val code: Int,
    val text: String,
): ResponseBody {
    /**
     * Semantics: https://github.com/jepsen-io/maelstrom/blob/main/doc/protocol.md#errors
     */
    enum class Code(val code: Int) {
        TIMEOUT(0),
        NODE_NOT_FOUND(1),
        NOT_SUPPORTED(10),
        TEMPORARILY_UNAVAILABLE(11),
        MALFORMED_REQUEST(12),
        CRASH(13),
        ABORT(14),
        KEY_DOES_NOT_EXIST(20),
        KEY_ALREADY_EXISTS(21),
        PRECONDITION_FAILED(22),
        TRANSACTION_CONFLICT(30),
    }
}
