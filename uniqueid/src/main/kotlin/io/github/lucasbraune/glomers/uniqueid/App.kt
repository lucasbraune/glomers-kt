package io.github.lucasbraune.glomers.uniqueid

import kotlinx.serialization.modules.plus
import io.github.lucasbraune.glomers.protocol.InitSerializersModule
import io.github.lucasbraune.glomers.protocol.InitService
import io.github.lucasbraune.glomers.protocol.NodeIO
import io.github.lucasbraune.glomers.protocol.request
import io.github.lucasbraune.glomers.protocol.serve
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
    serve(io) {
        request(initService::handle)
        request<Generate> { GenerateOk(generatorService.nextId(), it.body.msgId) }
    }
}
