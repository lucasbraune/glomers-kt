package glomers.app

import glomers.protocol.Echo
import glomers.protocol.EchoOk
import glomers.protocol.Init
import glomers.protocol.InitOk
import glomers.protocol.Message
import glomers.protocol.log
import glomers.protocol.send
import glomers.protocol.serve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

fun serveEcho() = runBlocking {
    val msgId = AtomicInteger()
    serve(Dispatchers.Default) {
        when (it.body) {
            is Init -> {
                send(Message(
                    src = it.dest,
                    dest = it.src,
                    body = InitOk(inReplyTo = it.body.msgId)
                ))
            }
            is Echo -> {
                send(Message(
                    src = it.dest,
                    dest = it.src,
                    body = EchoOk(
                        echo = it.body.echo,
                        msgId = msgId.getAndIncrement(),
                        inReplyTo = it.body.msgId,
                    )
                ))
            }
            else -> { log("Unexpected message: $it") }
        }
    }
}
