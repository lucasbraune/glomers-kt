package protocol2

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class Level {
    INFO, DEBUG, WARNING, ERROR
}

object Log {
    fun info(x: Any?) {
        log(Level.INFO, x)
    }

    fun debug(x: Any?) {
        log(Level.DEBUG, x)
    }

    fun warning(x: Any?) {
        log(Level.WARNING, x)
    }

    fun error(x: Any?) {
        log(Level.ERROR, x)
    }

    private fun log(level: Level, x: Any?) {
        val dateTime = dateTimeFormatter.format(LocalDateTime.now())
        System.err.println("${level.name} [${dateTime}] $x")
    }

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
}
