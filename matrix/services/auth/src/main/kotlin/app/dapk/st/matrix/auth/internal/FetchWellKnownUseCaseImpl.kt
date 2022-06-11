package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.http.MatrixHttpClient
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import java.nio.charset.Charset

internal typealias FetchWellKnownUseCase = suspend (String) -> WellKnownResult

internal class FetchWellKnownUseCaseImpl(
    private val httpClient: MatrixHttpClient,
    private val json: Json,
) : FetchWellKnownUseCase {

    override suspend fun invoke(domainUrl: String): WellKnownResult {
        return runCatching {
            val rawResponse = httpClient.execute(rawWellKnownRequestForServersWithoutContentTypes(domainUrl))
            json.decodeFromString(ApiWellKnown.serializer(), rawResponse.readString())
        }
            .fold(
                onSuccess = { WellKnownResult.Success(it) },
                onFailure = {
                    when (it) {
                        is UnknownHostException -> WellKnownResult.MissingWellKnown
                        is ClientRequestException -> when {
                            it.response.status.is404() -> WellKnownResult.MissingWellKnown
                            else -> WellKnownResult.Error(it)
                        }
                        is SerializationException -> WellKnownResult.InvalidWellKnown
                        else -> WellKnownResult.Error(it)
                    }
                },
            )
    }

    private fun rawWellKnownRequestForServersWithoutContentTypes(domainUrl: String) = wellKnownRequest(domainUrl)

}

sealed interface WellKnownResult {
    data class Success(val wellKnown: ApiWellKnown) : WellKnownResult
    object MissingWellKnown : WellKnownResult
    object InvalidWellKnown : WellKnownResult
    data class Error(val cause: Throwable) : WellKnownResult

}

fun HttpStatusCode.is404() = this.value == 404