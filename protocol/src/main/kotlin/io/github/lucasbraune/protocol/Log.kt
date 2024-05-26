package io.github.lucasbraune.protocol

import io.github.lucasbraune.protocol.Level.DEBUG
import io.github.lucasbraune.protocol.Level.ERROR
import io.github.lucasbraune.protocol.Level.INFO
import io.github.lucasbraune.protocol.Level.WARNING
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class Level {
    INFO, DEBUG, WARNING, ERROR
}

object Log {
    fun info(x: Any?) {
        log(INFO, x)
    }

    fun debug(x: Any?) {
        log(DEBUG, x)
    }

    fun warning(x: Any?) {
        log(WARNING, x)
    }

    fun error(x: Any?) {
        log(ERROR, x)
    }

    private fun log(level: Level, x: Any?) {
        val dateTime = dateTimeFormatter.format(LocalDateTime.now())
        System.err.println("${level.name} [${dateTime}] $x")
    }

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
}
