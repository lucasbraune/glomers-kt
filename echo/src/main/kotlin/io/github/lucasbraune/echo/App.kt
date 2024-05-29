package io.github.lucasbraune.echo

import kotlinx.serialization.modules.plus
import io.github.lucasbraune.protocol.Init
import io.github.lucasbraune.protocol.InitOk
import io.github.lucasbraune.protocol.InitSerializersModule
import io.github.lucasbraune.protocol.NodeIO
import io.github.lucasbraune.protocol.request
import io.github.lucasbraune.protocol.serveRoutes

suspend fun main() {
    val io = NodeIO(InitSerializersModule + EchoSerializersModule)
    serveRoutes(io) {
        request<Init> { InitOk(it.body.msgId) }
        request<Echo> { EchoOk(it.body.echo, it.body.msgId) }
    }
}
