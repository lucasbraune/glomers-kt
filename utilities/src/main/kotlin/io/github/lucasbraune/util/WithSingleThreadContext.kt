package io.github.lucasbraune.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
suspend fun <T> withSingleThreadContext(
    threadNamePrefix: String,
    block: suspend CoroutineScope.() -> T
) = newSingleThreadContext(threadNamePrefix).use { ctx ->
    withContext(ctx) { block() }
}
