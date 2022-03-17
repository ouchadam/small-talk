package app.dapk.st.work

import android.app.job.JobWorkItem
import app.dapk.st.work.WorkScheduler.WorkTask

interface TaskRunner {

    suspend fun run(tasks: List<RunnableWorkTask>): List<TaskResult>

    data class RunnableWorkTask(
        val source: JobWorkItem?,
        val task: WorkTask
    )

    sealed interface TaskResult {
        val source: JobWorkItem?

        data class Success(override val source: JobWorkItem?) : TaskResult
        data class Failure(override val source: JobWorkItem?, val canRetry: Boolean) : TaskResult
    }

}