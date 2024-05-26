package util

import kotlinx.coroutines.delay
import kotlin.time.Duration

data class RetryOptions(
    val retries: Int,
    val backoff: Duration = Duration.ZERO,
    val backoffMultiplier: Double = 1.0,
    val maxBackoff: Duration = Duration.INFINITE,
)

suspend fun <T> retry(
    options: RetryOptions,
    block: suspend () -> T,
): T {
    var backoff = options.backoff
    repeat(options.retries) {
        try {
            return block()
        } catch (ex: Exception) {
            delay(backoff)
            backoff *= options.backoffMultiplier
            if (backoff > options.maxBackoff) {
                backoff = options.maxBackoff
            }
        }
    }
    return block()
}
