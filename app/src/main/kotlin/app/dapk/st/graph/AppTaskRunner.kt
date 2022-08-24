package app.dapk.st.graph

import app.dapk.st.matrix.push.PushService
import app.dapk.st.push.PushTokenPayload
import app.dapk.st.work.TaskRunner
import io.ktor.client.plugins.*
import kotlinx.serialization.json.Json

class AppTaskRunner(
    private val pushService: PushService,
) {

    suspend fun run(workTask: TaskRunner.RunnableWorkTask): TaskRunner.TaskResult {
        return when (val type = workTask.task.type) {
            "push_token" -> {
                runCatching {
                    val payload = Json.decodeFromString(PushTokenPayload.serializer(), workTask.task.jsonPayload)
                    pushService.registerPush(payload.token, payload.gatewayUrl)
                }.fold(
                    onSuccess = { TaskRunner.TaskResult.Success(workTask.source) },
                    onFailure = {
                        val canRetry = if (it is ClientRequestException) {
                            it.response.status.value !in (400 until 500)
                        } else {
                            true
                        }
                        TaskRunner.TaskResult.Failure(workTask.source, canRetry = canRetry)
                    }
                )
            }

            else -> throw IllegalArgumentException("Unknown work type: $type")
        }

    }

}
