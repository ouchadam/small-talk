package app.dapk.st.matrix

import app.dapk.st.matrix.MatrixTaskRunner.MatrixTask
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.http.MatrixHttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModuleBuilder

class MatrixClient(
    private val httpClientFactory: MatrixHttpClient.Factory,
    private val logger: MatrixLogger,
) : MatrixServiceProvider {

    private val serviceInstaller = ServiceInstaller()

    fun install(scope: MatrixServiceInstaller.() -> Unit) {
        serviceInstaller.install(httpClientFactory, logger, scope)
    }

    override fun <T : MatrixService> getService(key: ServiceKey): T {
        return serviceInstaller.getService(key)
    }

    suspend fun run(task: MatrixTask): MatrixTaskRunner.TaskResult {
        return serviceInstaller.delegate(task)
    }
}

typealias ServiceKey = Any

interface MatrixService {
    fun interface Factory {
        fun create(deps: ServiceDependencies): Pair<ServiceKey, MatrixService>
    }
}

data class ServiceDependencies(
    val httpClient: MatrixHttpClient,
    val json: Json,
    val services: MatrixServiceProvider,
    val logger: MatrixLogger,
)

interface MatrixServiceInstaller {
    fun serializers(builder: SerializersModuleBuilder.() -> Unit)
    fun <T : MatrixService> install(factory: MatrixService.Factory): InstallExtender<T>
}

interface InstallExtender<T : MatrixService> {
    fun proxy(proxy: (T) -> T)
}

interface MatrixServiceProvider {
    fun <T : MatrixService> getService(key: ServiceKey): T
}

fun interface ServiceDepFactory<T> {
    fun create(services: MatrixServiceProvider): T
}

interface MatrixTaskRunner {
    suspend fun canRun(task: MatrixTask): Boolean = false
    suspend fun run(task: MatrixTask): TaskResult = throw IllegalArgumentException("Should only be invoked if canRun == true")

    data class MatrixTask(val type: String, val jsonPayload: String)

    sealed interface TaskResult {
        object Success : TaskResult
        data class Failure(val canRetry: Boolean) : TaskResult
    }

}
