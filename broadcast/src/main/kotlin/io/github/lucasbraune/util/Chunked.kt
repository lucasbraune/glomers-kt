package io.github.lucasbraune.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.whileSelect
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CoroutineScope.batches(
    channel: ReceiveChannel<T>,
    timeBetweenBatches: Duration,
): ReceiveChannel<List<T>> =
    produce {
        while (true) {
            val chunk = mutableListOf<T>()
            val timer = async { delay(timeBetweenBatches) }
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
