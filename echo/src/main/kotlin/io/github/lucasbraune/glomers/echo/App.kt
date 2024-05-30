package io.github.lucasbraune.glomers.echo

import kotlinx.serialization.modules.plus
import io.github.lucasbraune.glomers.protocol.Init
import io.github.lucasbraune.glomers.protocol.InitOk
import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve

suspend fun main() {
    val io = NodeIO(InitSerializersModule + EchoSerializersModule)
    serve(io) {
        request<Init> { InitOk(it.body.msgId) }
        request<Echo> { EchoOk(it.body.echo, it.body.msgId) }
    }
}
