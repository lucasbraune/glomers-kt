package protocol2

import java.time.Instant
import kotlin.time.Duration

private enum class Level {
    WARNING, ERROR
}

object Log {
    fun warning(x: Any?) {
        log(Level.WARNING, x)
    }
    fun error(x: Any?) {
        log(Level.ERROR, x)
    }
    private fun log(level: Level, x: Any?) {
        Duration
        System.err.println("${level.name} ${Instant.now()}: $x")
    }
}
