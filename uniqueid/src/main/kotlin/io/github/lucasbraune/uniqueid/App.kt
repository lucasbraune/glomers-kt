package io.github.lucasbraune.uniqueid

import kotlinx.serialization.modules.plus
import io.github.lucasbraune.protocol.InitSerializersModule
import io.github.lucasbraune.protocol.InitService
import io.github.lucasbraune.protocol.NodeIO
import io.github.lucasbraune.protocol.request
import io.github.lucasbraune.protocol.serveRoutes
import java.util.concurrent.atomic.AtomicInteger

class GeneratorService(
    private val initService: InitService,
) {
    private val generatedIdsCount = AtomicInteger()

    suspend fun nextId(): Int {
        val nodeIds = initService.nodeIds()
        val index = nodeIds.indexOf(initService.nodeId())
        return generatedIdsCount.getAndIncrement() * nodeIds.size + index
    }
}

suspend fun main() {
    val io = NodeIO(InitSerializersModule + UniqueIdsSerializersModule)
    val initService = InitService()
    val generatorService = GeneratorService(initService)
    serveRoutes(io) {
        request(initService::handle)
        request<Generate> { GenerateOk(generatorService.nextId(), it.body.msgId) }
    }
}
