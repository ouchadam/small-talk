package app.dapk.st.matrix.http.ktor

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.ktor.internal.KtorMatrixHttpClient
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class KtorMatrixHttpClientFactory(
    private val credentialsStore: CredentialsStore,
    private val includeLogging: Boolean,
) : MatrixHttpClient.Factory {

    override fun create(jsonInstance: Json): MatrixHttpClient {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(jsonInstance)
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