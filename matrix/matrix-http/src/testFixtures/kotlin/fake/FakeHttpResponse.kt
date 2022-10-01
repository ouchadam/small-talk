package fake

import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk

class FakeHttpResponse {

    val instance = mockk<HttpResponse>(relaxed = true)

    fun givenStatus(code: Int) {
        every { instance.status } returns HttpStatusCode(code, "")
    }

}