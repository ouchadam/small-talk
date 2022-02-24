package app.dapk.st.matrix.http.ktor.internal

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.Method
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class KtorMatrixHttpClient(
    private val client: HttpClient,
    private val tokenProvider: CredentialsStore
) : MatrixHttpClient {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> execute(request: MatrixHttpClient.HttpRequest<T>): T {
        return when {
            !request.authenticated -> {
                request.execute { buildRequest(credentials = null, request) }
            }
            else -> authenticatedRequest(request)
        }
    }

    private suspend fun <T> authenticatedRequest(request: MatrixHttpClient.HttpRequest<T>) =
        when (val initialCredentials = tokenProvider.credentials()) {
            null -> {
                val credentials = authenticate()
                request.execute {
                    buildAuthenticatedRequest(
                        request,
                        credentials
                    )
                }
            }
            else -> withTokenRetry(initialCredentials) { token ->
                request.execute {
                    buildAuthenticatedRequest(
                        request,
                        token
                    )
                }
            }
        }

    private suspend fun <T> withTokenRetry(originalCredentials: UserCredentials, request: suspend (UserCredentials) -> T): T {
        return try {
            request(originalCredentials)
        } catch (error: ClientRequestException) {
            if (error.response.status.value == 401) {
                val token = authenticate()
                request(token)
            } else {
                throw error
            }
        }
    }


    suspend fun authenticate(): UserCredentials {
        throw Error() // TODO
//        val tokenResult = client.request<String> { buildRequest(AuthEndpoint.anonAccessToken()) }
//        tokenProvider.update(tokenResult.accessToken)
//        return tokenResult.accessToken
    }

    private fun <T> HttpRequestBuilder.buildRequest(
        credentials: UserCredentials?,
        request: MatrixHttpClient.HttpRequest<T>
    ) {
        val host =
            request.baseUrl ?: credentials?.homeServer?.value ?: throw Error()
        this.url("$host${request.path}")
        this.method = when (request.method) {
            Method.GET -> HttpMethod.Get
            Method.POST -> HttpMethod.Post
            Method.DELETE -> HttpMethod.Delete
            Method.PUT -> HttpMethod.Put
        }
        this.headers.apply {
            request.headers.forEach {
                append(it.first, it.second)
            }
        }
        this.body = request.body
    }

    private fun <T> HttpRequestBuilder.buildAuthenticatedRequest(
        request: MatrixHttpClient.HttpRequest<T>,
        credentials: UserCredentials
    ) {
        this.buildRequest(credentials, request)
        this.headers.apply {
            append(HttpHeaders.Authorization, "Bearer ${credentials.accessToken}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> MatrixHttpClient.HttpRequest<T>.execute(requestBuilder: HttpRequestBuilder.() -> Unit): T {
        return client.request<HttpResponse> { requestBuilder(this) }.call.receive(this.typeInfo) as T
    }

}
