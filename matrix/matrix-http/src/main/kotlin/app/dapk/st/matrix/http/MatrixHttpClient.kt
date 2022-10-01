package app.dapk.st.matrix.http

import io.ktor.client.utils.*
import io.ktor.http.content.*
import io.ktor.util.reflect.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

interface MatrixHttpClient {

    suspend fun <T : Any> execute(request: HttpRequest<T>): T

    data class HttpRequest<T> constructor(
        val path: String,
        val method: Method,
        val body: OutgoingContent = EmptyContent,
        val headers: List<Pair<String, String>> = emptyList(),
        val authenticated: Boolean = true,
        val setAcceptLanguage: Boolean = true,
        val baseUrl: String? = null,
        val typeInfo: TypeInfo,
    ) {

        companion object {
            inline fun <reified T> httpRequest(
                path: String,
                method: Method,
                body: OutgoingContent = EmptyContent,
                headers: List<Pair<String, String>> = emptyList(),
                authenticated: Boolean = true,
                setAcceptLanguage: Boolean = true,
                baseUrl: String? = null,
            ) = HttpRequest<T>(
                path,
                method,
                body,
                headers,
                authenticated,
                setAcceptLanguage,
                baseUrl,
                typeInfo = typeInfo<T>()
            )
        }

    }

    enum class Method { GET, POST, DELETE, PUT }

    companion object {
        val json = Json
        @OptIn(ExperimentalSerializationApi::class)
        val jsonWithDefaults = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }

    fun interface Factory {
        fun create(json: Json): MatrixHttpClient
    }
}
