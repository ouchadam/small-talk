package fixture

import fake.FakeHttpResponse
import io.ktor.client.plugins.*

fun a404HttpError() = ClientRequestException(
    FakeHttpResponse().apply { givenStatus(404) }.instance,
    cachedResponseText = ""
)

fun a403HttpError() = ClientRequestException(
    FakeHttpResponse().apply { givenStatus(403) }.instance,
    cachedResponseText = ""
)
