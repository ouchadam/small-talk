package app.dapk.st.work

import android.content.Context
import app.dapk.st.core.ProvidableModule

class WorkModule(private val context: Context) {
    fun workScheduler(): WorkScheduler = WorkSchedulingJobScheduler(context)
}

class TaskRunnerModule(private val taskRunner: TaskRunner) : ProvidableModule {
    fun taskRunner() = taskRunner
}