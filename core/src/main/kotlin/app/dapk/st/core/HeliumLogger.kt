package app.dapk.st.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

enum class AppLogTag(val key: String) {
    NOTIFICATION("notification"),
    PERFORMANCE("performance"),
    PUSH("push"),
    ERROR_NON_FATAL("error - non fatal"),
}

typealias AppLogger = (tag: String, message: String) -> Unit

private var appLoggerInstance: AppLogger? = null

fun attachAppLogger(logger: AppLogger) {
    appLoggerInstance = logger
}

fun log(tag: AppLogTag, message: Any) {
    appLoggerInstance?.invoke(tag.key, message.toString())
}

suspend fun <T> logP(area: String, block: suspend () -> T): T {
    val start = System.currentTimeMillis()
    return block().also {
        val timeTaken = System.currentTimeMillis() - start
        log(AppLogTag.PERFORMANCE, "$area: took $timeTaken ms")
    }
}

fun <T> Flow<T>.logP(area: String): Flow<T> {
    var start = -1L
    return this
        .onStart { start = System.currentTimeMillis() }
        .onCompletion {
            val timeTaken = System.currentTimeMillis() - start
            log(AppLogTag.PERFORMANCE, "$area: took $timeTaken ms")
        }
}