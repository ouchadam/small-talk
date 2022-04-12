package app.dapk.st.matrix.http.ktor

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ktor.internal.KtorMatrixHttpClient
import io.ktor.client.*
import io.ktor.client.plugins.json.*
import io.ktor.client.plugins.kotlinx.serializer.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.json.Json

class KtorMatrixHttpClientFactory(
    private val credentialsStore: CredentialsStore,
    private val includeLogging: Boolean,
) : MatrixHttpClient.Factory {

    override fun create(json: Json): MatrixHttpClient {
        val client = HttpClient {
            install(JsonPlugin) {
                serializer = KotlinxSerializer(json)
            }
            expectSuccess = true
            if (includeLogging) {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.ALL
                }
            }
        }
        return KtorMatrixHttpClient(client, credentialsStore)
    }

}