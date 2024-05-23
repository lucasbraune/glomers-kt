package protocol2

import kotlinx.serialization.Serializable

interface MessageBody

@Serializable
data class Message<Body : MessageBody>(
    val src: String,
    val dest: String,
    val body: Body
)
