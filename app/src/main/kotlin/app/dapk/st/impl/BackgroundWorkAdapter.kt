package app.dapk.st.impl

import app.dapk.st.engine.BackgroundScheduler
import app.dapk.st.work.WorkScheduler

class BackgroundWorkAdapter(private val workScheduler: WorkScheduler) : BackgroundScheduler {
    override fun schedule(key: String, task: BackgroundScheduler.Task) {
        workScheduler.schedule(
            WorkScheduler.WorkTask(
                jobId = 1,
                type = task.type,
                jsonPayload = task.jsonPayload.value,
            )
        )
    }
}