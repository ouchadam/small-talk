package app.dapk.st.matrix

import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.http.MatrixHttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder

internal class ServiceInstaller {

    private val services = mutableMapOf<Any, MatrixService>()
    private val serviceInstaller = object : MatrixServiceInstaller {

        val serviceCollector = mutableListOf<Pair<MatrixService.Factory, (MatrixService) -> MatrixService>>()
        val serializers = mutableListOf<SerializersModuleBuilder.() -> Unit>()

        override fun serializers(builder: SerializersModuleBuilder.() -> Unit) {
            serializers.add(builder)
        }

        override fun <T : MatrixService> install(factory: MatrixService.Factory): InstallExtender<T> {
            val mutableProxy = MutableProxy<T>()
            return object : InstallExtender<T> {
                override fun proxy(proxy: (T) -> T) {
                    mutableProxy.value = proxy
                }
            }.also {
                serviceCollector.add(factory to mutableProxy)
            }
        }
    }

    fun install(httpClientFactory: MatrixHttpClient.Factory, logger: MatrixLogger, scope: MatrixServiceInstaller.() -> Unit) {
        scope(serviceInstaller)
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                serviceInstaller.serializers.forEach {
                    it.invoke(this)
                }
            }
        }

        val httpClient = httpClientFactory.create(json)
        val serviceProvider = object : MatrixServiceProvider {
            override fun <T : MatrixService> getService(key: ServiceKey) = this@ServiceInstaller.getService<T>(key)
        }
        serviceInstaller.serviceCollector.forEach { (factory, extender) ->
            val (key, service) = factory.create(ServiceDependencies(httpClient, json, serviceProvider, logger))
            services[key] = extender(service)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : MatrixService> getService(key: ServiceKey): T {
        return services[key] as T
    }

    suspend fun delegate(task: MatrixTaskRunner.MatrixTask): MatrixTaskRunner.TaskResult {
        return services.values
            .filterIsInstance<MatrixTaskRunner>()
            .firstOrNull { it.canRun(task) }?.run(task)
            ?: throw IllegalArgumentException("No service available to handle ${task.type}")
    }

}

internal class MutableProxy<T : MatrixService> : (MatrixService) -> MatrixService {

    var value: (T) -> T = { it }

    @Suppress("UNCHECKED_CAST")
    override fun invoke(service: MatrixService) = value(service as T)

}