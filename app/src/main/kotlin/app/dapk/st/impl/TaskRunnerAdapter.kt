package app.dapk.st.impl

import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.ChatEngineTask
import app.dapk.st.work.TaskRunner

class TaskRunnerAdapter(
    private val chatEngine: ChatEngine,
    private val appTaskRunner: AppTaskRunner,
) : TaskRunner {

    override suspend fun run(tasks: List<TaskRunner.RunnableWorkTask>): List<TaskRunner.TaskResult> {
        return tasks.map {
            when {
                it.task.type.startsWith("matrix") -> {
                    when (val result = chatEngine.runTask(ChatEngineTask(it.task.type, it.task.jsonPayload))) {
                        is app.dapk.st.engine.TaskRunner.TaskResult.Failure -> TaskRunner.TaskResult.Failure(it.source, canRetry = result.canRetry)
                        app.dapk.st.engine.TaskRunner.TaskResult.Success -> TaskRunner.TaskResult.Success(it.source)
                    }
                }

                else -> appTaskRunner.run(it)
            }
        }
    }
}