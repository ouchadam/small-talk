package app.dapk.st.graph

import app.dapk.st.matrix.MatrixTaskRunner
import app.dapk.st.work.TaskRunner

class TaskRunnerAdapter(
    private val matrixTaskRunner: suspend (MatrixTaskRunner.MatrixTask) -> MatrixTaskRunner.TaskResult,
    private val appTaskRunner: AppTaskRunner,
) : TaskRunner {

    override suspend fun run(tasks: List<TaskRunner.RunnableWorkTask>): List<TaskRunner.TaskResult> {
        return tasks.map {
            when {
                it.task.type.startsWith("matrix") -> {
                    when (val result = matrixTaskRunner(MatrixTaskRunner.MatrixTask(it.task.type, it.task.jsonPayload))) {
                        is MatrixTaskRunner.TaskResult.Failure -> TaskRunner.TaskResult.Failure(it.source, canRetry = result.canRetry)
                        MatrixTaskRunner.TaskResult.Success -> TaskRunner.TaskResult.Success(it.source)
                    }
                }
                else -> appTaskRunner.run(it)
            }
        }
    }
}