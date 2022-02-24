package app.dapk.st.matrix.crypto.internal

import fake.FakeCredentialsStore
import fake.FakeDeviceService
import fake.FakeOlm
import fixture.aUserCredentials
import fixture.anAccountCryptoSession
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.expect

private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession()
private val A_USER_CREDENTIALS = aUserCredentials()

class FetchAccountCryptoUseCaseTest {

    private val credentialsStore = FakeCredentialsStore()
    private val olm = FakeOlm()
    private val deviceService = FakeDeviceService()

    private val fetchAccountCryptoUseCase = FetchAccountCryptoUseCaseImpl(
        credentialsStore,
        olm,
        deviceService,
    )

    @Test
    fun `when creating an account crypto session then also uploads device keys`() = runTest {
        credentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        olm.givenCreatesAccount(A_USER_CREDENTIALS).returns(AN_ACCOUNT_CRYPTO_SESSION)
        deviceService.expect { it.uploadDeviceKeys(AN_ACCOUNT_CRYPTO_SESSION.deviceKeys) }

        val result = fetchAccountCryptoUseCase.invoke()

        result shouldBeEqualTo AN_ACCOUNT_CRYPTO_SESSION
    }

    @Test
    fun `when fetching an existing crypto session then returns`() = runTest {
        credentialsStore.givenCredentials().returns(A_USER_CREDENTIALS)
        olm.givenAccount(A_USER_CREDENTIALS).returns(AN_ACCOUNT_CRYPTO_SESSION)

        val result = fetchAccountCryptoUseCase.invoke()

        result shouldBeEqualTo AN_ACCOUNT_CRYPTO_SESSION
    }
}
