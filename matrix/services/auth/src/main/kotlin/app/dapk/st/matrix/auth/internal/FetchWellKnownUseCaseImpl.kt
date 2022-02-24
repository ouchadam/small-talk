package app.dapk.st.matrix.auth.internal

import app.dapk.st.matrix.http.MatrixHttpClient
import kotlinx.serialization.json.Json

internal typealias FetchWellKnownUseCase = suspend (String) -> ApiWellKnown

internal class FetchWellKnownUseCaseImpl(
    private val httpClient: MatrixHttpClient,
    private val json: Json,
) : FetchWellKnownUseCase {

    override suspend fun invoke(domainUrl: String): ApiWellKnown {
        // workaround for matrix.org not returning a content-type
        val raw = httpClient.execute(wellKnownRequest(domainUrl))
        return json.decodeFromString(ApiWellKnown.serializer(), raw)
    }

}