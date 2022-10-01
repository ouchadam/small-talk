package fake

import app.dapk.st.matrix.http.MatrixHttpClient
import io.ktor.client.plugins.*
import io.mockk.coEvery
import io.mockk.mockk

class FakeMatrixHttpClient : MatrixHttpClient by mockk() {
    fun <T : Any> given(request: MatrixHttpClient.HttpRequest<T>, response: T) {
        coEvery { execute(request) } returns response
    }

    fun <T : Any> errors(request: MatrixHttpClient.HttpRequest<T>, cause: Throwable) {
        coEvery { execute(request) } throws cause
    }
}
