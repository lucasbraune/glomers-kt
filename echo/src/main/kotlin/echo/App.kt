package echo

import kotlinx.serialization.modules.plus
import protocol2.Init
import protocol2.InitOk
import protocol2.InitSerializersModule
import protocol2.MessageIO
import protocol2.request
import protocol2.serveRoutes

suspend fun main() {
    val io = MessageIO(InitSerializersModule + EchoSerializersModule)
    serveRoutes(io) {
        request<Init> { InitOk(it.body.msgId) }
        request<Echo> { EchoOk(it.body.echo, it.body.msgId) }
    }
}
