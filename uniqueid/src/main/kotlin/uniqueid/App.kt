package uniqueid

import kotlinx.serialization.modules.plus
import protocol2.InitSerializersModule
import protocol2.InitService
import protocol2.MessageIO
import protocol2.request
import protocol2.serveRoutes
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
    val io = MessageIO(InitSerializersModule + UniqueIdsSerializersModule)
    val initService = InitService()
    val generatorService = GeneratorService(initService)
    serveRoutes(io) {
        request(initService::handle)
        request<Generate> { GenerateOk(generatorService.nextId(), it.body.msgId) }
    }
}
