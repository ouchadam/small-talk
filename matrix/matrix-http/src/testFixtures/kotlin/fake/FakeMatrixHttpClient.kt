package fake

import app.dapk.st.matrix.http.MatrixHttpClient
import io.mockk.coEvery
import io.mockk.mockk

class FakeMatrixHttpClient : MatrixHttpClient by mockk() {
    fun <T : Any> given(request: MatrixHttpClient.HttpRequest<T>, response: T) {
        coEvery { execute(request) } returns response
    }
}