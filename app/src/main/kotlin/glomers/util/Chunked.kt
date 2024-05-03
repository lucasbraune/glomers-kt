import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.*

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CoroutineScope.chunk(channel: ReceiveChannel<T>, milliseconds: Long): ReceiveChannel<List<T>> =
    produce {
        while (true) {
            val chunk = mutableListOf<T>()
            val timer = async { delay(milliseconds) }
            whileSelect {
                channel.onReceiveCatching {
                    if (it.isSuccess) {
                        chunk += it.getOrThrow()
                    }
                    it.isSuccess
                }
                timer.onAwait {
                    false
                }
            }
            if (chunk.isNotEmpty()) {
                send(chunk)
            }
        }
    }

//fun <T> ReceiveChannel<T>.chunked(size: Int, time: Long) =
//    produce<List<T>>(onCompletion = consumes()) {
//        while (true) { // this loop goes over each chunk
//            val chunk = mutableListOf<T>() // current chunk
//            val ticker = ticker(time) // time-limit for this chunk
//            try {
//                whileSelect {
//                    ticker.onReceive {
//                        false  // done with chunk when timer ticks, takes priority over received elements
//                    }
//                    this@chunked.onReceive {
//                        chunk += it
//                        chunk.size < size // continue whileSelect if chunk is not full
//                    }
//                }
//            } catch (e: ClosedReceiveChannelException) {
//                return@produce // that is normal exception when the source channel is over -- just stop
//            } finally {
//                ticker.cancel() // release ticker (we don't need it anymore as we wait for the first tick only)
//                if (chunk.isNotEmpty()) send(chunk) // send non-empty chunk on exit from whileSelect
//            }
//        }
//    }