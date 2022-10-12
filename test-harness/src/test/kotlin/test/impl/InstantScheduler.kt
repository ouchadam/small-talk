package test.impl

import app.dapk.st.matrix.MatrixClient
import app.dapk.st.matrix.MatrixTaskRunner
import app.dapk.st.matrix.message.BackgroundScheduler
import kotlinx.coroutines.runBlocking

class InstantScheduler(private val matrixClient: MatrixClient) : BackgroundScheduler {

    override fun schedule(key: String, task: BackgroundScheduler.Task) {
        runBlocking {
            matrixClient.run(
                MatrixTaskRunner.MatrixTask(
                    task.type,
                    task.jsonPayload.value,
                )
            )
        }
    }

}