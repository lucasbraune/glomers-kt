package io.github.lucasbraune.glomers.broadcast

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope {
    val node = BroadcastNode()
    launch { node.work() }
    node.serve()
}
