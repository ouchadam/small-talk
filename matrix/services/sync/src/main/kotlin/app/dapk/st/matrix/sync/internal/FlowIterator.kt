package app.dapk.st.matrix.sync.internal

import app.dapk.st.matrix.common.MatrixLogTag.SYNC
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.matrixLog
import kotlinx.coroutines.*

internal class SideEffectFlowIterator(private val logger: MatrixLogger) {
    suspend fun <T> loop(initial: T?, action: suspend (T?) -> T?) {
        var previousState = initial

        while (currentCoroutineContext().isActive) {
            logger.matrixLog(SYNC, "loop iteration")
            try {
                previousState = withContext(NonCancellable) {
                    action(previousState)
                }
            } catch (error: Throwable) {
                logger.matrixLog(SYNC, "on loop error: ${error.message}")
                error.printStackTrace()
                delay(10000L)
            }
        }
        logger.matrixLog(SYNC, "isActive: ${currentCoroutineContext().isActive}")
    }
}