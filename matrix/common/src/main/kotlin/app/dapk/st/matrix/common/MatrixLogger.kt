package app.dapk.st.matrix.common

enum class MatrixLogTag(val key: String) {
    MATRIX("matrix"),
    CRYPTO("crypto"),
    SYNC("sync"),
    VERIFICATION("verification"),
    PERF("performance"),
    ROOM("room"),
}

typealias MatrixLogger = (tag: String, message: String) -> Unit

fun MatrixLogger.crypto(message: Any) = this.matrixLog(MatrixLogTag.CRYPTO, message)

fun MatrixLogger.matrixLog(tag: MatrixLogTag, message: Any) {
    this.invoke(tag.key, message.toString())
}

fun MatrixLogger.matrixLog(message: Any) {
    matrixLog(tag = MatrixLogTag.MATRIX, message = message)
}

fun MatrixLogger.logP(area: String): PerfTracker {
    val start = System.currentTimeMillis()
    var lastCheckpoint = start
    return object : PerfTracker {
        override fun checkpoint(label: String) {
            val now = System.currentTimeMillis()
            val timeTaken = (now - lastCheckpoint)
            lastCheckpoint = now
            matrixLog(MatrixLogTag.PERF, "$area - $label: took $timeTaken ms")
        }

        override fun stop() {
            val timeTaken = System.currentTimeMillis() - start
            matrixLog(MatrixLogTag.PERF, "$area: took $timeTaken ms")
        }
    }
}

interface PerfTracker {
    fun checkpoint(label: String)
    fun stop()
}

inline fun <T> MatrixLogger.logP(area: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    return block().also {
        val timeTaken = System.currentTimeMillis() - start
        matrixLog(MatrixLogTag.PERF, "$area: took $timeTaken ms")
    }
}