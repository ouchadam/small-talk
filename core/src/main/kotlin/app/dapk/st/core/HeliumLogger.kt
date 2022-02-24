package app.dapk.st.core

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
