package app.dapk.st.matrix.room.internal

import app.dapk.st.matrix.room.ProfileService
import fake.FakeCredentialsStore
import fake.FakeMatrixHttpClient
import fixture.a403HttpError
import fixture.a404HttpError
import fixture.aUserCredentials
import fixture.aUserId
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.coInvoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.junit.Test

private val A_USER_CREDENTIALS = aUserCredentials()
private val A_USER_ID = aUserId()
private val AN_API_ME_RESPONSE = ApiMe(
    displayName = "a display name",
    avatarUrl = null,
)
private val AN_UNHANDLED_ERROR = RuntimeException()

class FetchMeUseCaseTest {

    private val fakeHttpClient = FakeMatrixHttpClient()
    private val fakeCredentialsStore = FakeCredentialsStore()

    private val useCase = FetchMeUseCase(fakeHttpClient, fakeCredentialsStore)

    @Test
    fun `when fetching me, then returns Me instance`() = runTest {
        fakeCredentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        fakeHttpClient.given(profileRequest(aUserId()), AN_API_ME_RESPONSE)

        val result = useCase.fetchMe()

        result shouldBeEqualTo ProfileService.Me(
            userId = A_USER_ID,
            displayName = AN_API_ME_RESPONSE.displayName,
            avatarUrl = null,
            homeServerUrl = fakeCredentialsStore.credentials()!!.homeServer
        )
    }

    @Test
    fun `given unhandled error, when fetching me, then throws`() = runTest {
        fakeCredentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        fakeHttpClient.errors(profileRequest(aUserId()), AN_UNHANDLED_ERROR)

        coInvoking { useCase.fetchMe() } shouldThrow AN_UNHANDLED_ERROR
    }

    @Test
    fun `given 403, when fetching me, then throws`() = runTest {
        val error = a403HttpError()
        fakeCredentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        fakeHttpClient.errors(profileRequest(aUserId()), error)

        coInvoking { useCase.fetchMe() } shouldThrow error
    }

    @Test
    fun `given 404, when fetching me, then returns Me instance with empty profile fields`() = runTest {
        fakeCredentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        fakeHttpClient.errors(profileRequest(aUserId()), a404HttpError())

        val result = useCase.fetchMe()

        result shouldBeEqualTo ProfileService.Me(
            userId = A_USER_ID,
            displayName = null,
            avatarUrl = null,
            homeServerUrl = fakeCredentialsStore.credentials()!!.homeServer
        )
    }

}