package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.ServerKeyCount
import app.dapk.st.matrix.device.DeviceService
import fake.FakeCredentialsStore
import fake.FakeDeviceService
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.aUserCredentials
import fixture.anAccountCryptoSession
import internalfake.FakeFetchAccountCryptoUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.expect

private const val MAX_KEYS = 100
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession(maxKeys = MAX_KEYS)
private val A_USER_CREDENTIALS = aUserCredentials()
private val GENERATED_ONE_TIME_KEYS = DeviceService.OneTimeKeys(listOf())

class MaybeCreateAndUploadOneTimeKeysUseCaseTest {

    private val fakeDeviceService = FakeDeviceService()
    private val fakeOlm = FakeOlm()
    private val fakeCredentialsStore = FakeCredentialsStore().also { it.givenCredentials().returns(A_USER_CREDENTIALS) }
    private val fakeFetchAccountCryptoUseCase = FakeFetchAccountCryptoUseCase()

    private val maybeCreateAndUploadOneTimeKeysUseCase = MaybeCreateAndUploadOneTimeKeysUseCaseImpl(
        fakeFetchAccountCryptoUseCase.also { it.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION) },
        fakeOlm,
        fakeCredentialsStore,
        fakeDeviceService,
        FakeMatrixLogger(),
    )

    @Test
    fun `given more keys than the current max then does nothing`() = runTest {
        val moreThanHalfOfMax = ServerKeyCount((MAX_KEYS / 2) + 1)

        maybeCreateAndUploadOneTimeKeysUseCase.invoke(moreThanHalfOfMax)

        fakeDeviceService.verifyDidntUploadOneTimeKeys()
    }

    @Test
    fun `given account has keys and server count is 0 then does nothing`() = runTest {
        fakeFetchAccountCryptoUseCase.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION.copy(hasKeys = true))
        val zeroServiceKeys = ServerKeyCount(0)

        maybeCreateAndUploadOneTimeKeysUseCase.invoke(zeroServiceKeys)

        fakeDeviceService.verifyDidntUploadOneTimeKeys()
    }

    @Test
    fun `given 0 current keys than generates and uploads 75 percent of the max key capacity`() = runTest {
        fakeDeviceService.expect { it.uploadOneTimeKeys(GENERATED_ONE_TIME_KEYS) }
        val keysToGenerate = (MAX_KEYS * 0.75f).toInt()
        fakeOlm.givenGeneratesOneTimeKeys(AN_ACCOUNT_CRYPTO_SESSION, keysToGenerate, A_USER_CREDENTIALS).returns(GENERATED_ONE_TIME_KEYS)

        maybeCreateAndUploadOneTimeKeysUseCase.invoke(ServerKeyCount(0))
    }

    @Test
    fun `given less than half of max current keys than generates and uploads 25 percent plus delta from half of the max key capacity`() = runTest {
        val deltaFromHalf = 5
        val lessThanHalfOfMax = ServerKeyCount((MAX_KEYS / 2) - deltaFromHalf)
        val keysToGenerate = (MAX_KEYS * 0.25).toInt() + deltaFromHalf
        fakeDeviceService.expect { it.uploadOneTimeKeys(GENERATED_ONE_TIME_KEYS) }
        fakeOlm.givenGeneratesOneTimeKeys(AN_ACCOUNT_CRYPTO_SESSION, keysToGenerate, A_USER_CREDENTIALS).returns(GENERATED_ONE_TIME_KEYS)

        maybeCreateAndUploadOneTimeKeysUseCase.invoke(lessThanHalfOfMax)
    }
}