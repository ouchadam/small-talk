package app.dapk.st.matrix.crypto.internal

import fake.FakeDeviceService
import fake.FakeMatrixLogger
import fixture.*
import internalfake.FakeFetchAccountCryptoUseCase
import internalfake.FakeRegisterOlmSessionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test

private val USERS_TO_UPDATE = listOf(aUserId())
private val A_SYNC_TOKEN = aSyncToken()
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession(deviceKeys = aDeviceKeys(deviceId = aDeviceId("unique-device-id")))
private val A_DEVICE_KEYS = listOf(aDeviceKeys())
private val OWN_DEVICE_KEYS = listOf(AN_ACCOUNT_CRYPTO_SESSION.deviceKeys)
private val IGNORED_REGISTERED_SESSION = listOf(aDeviceCryptoSession())

internal class UpdateKnownOlmSessionUseCaseTest {

    private val fakeFetchAccountCryptoUseCase = FakeFetchAccountCryptoUseCase()
    private val fakeDeviceService = FakeDeviceService()
    private val fakeRegisterOlmSessionUseCase = FakeRegisterOlmSessionUseCase()

    private val updateKnownOlmSessionUseCase = UpdateKnownOlmSessionUseCaseImpl(
        fakeFetchAccountCryptoUseCase,
        fakeDeviceService,
        fakeRegisterOlmSessionUseCase,
        FakeMatrixLogger()
    )

    @Test
    fun `when updating know olm sessions, then registers device keys`() = runTest {
        fakeFetchAccountCryptoUseCase.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION)
        fakeDeviceService.givenFetchesDevices(USERS_TO_UPDATE, A_SYNC_TOKEN).returns(A_DEVICE_KEYS)
        fakeRegisterOlmSessionUseCase.givenRegistersSessions(A_DEVICE_KEYS, AN_ACCOUNT_CRYPTO_SESSION).returns(IGNORED_REGISTERED_SESSION)

        updateKnownOlmSessionUseCase.invoke(USERS_TO_UPDATE, A_SYNC_TOKEN)

        fakeRegisterOlmSessionUseCase.verifyRegistersKeys(A_DEVICE_KEYS, AN_ACCOUNT_CRYPTO_SESSION)
    }

    @Test
    fun `given device keys contains own device, when updating known olm session, then skips registering`() = runTest {
        fakeFetchAccountCryptoUseCase.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION)
        fakeDeviceService.givenFetchesDevices(USERS_TO_UPDATE, A_SYNC_TOKEN).returns(OWN_DEVICE_KEYS)

        updateKnownOlmSessionUseCase.invoke(USERS_TO_UPDATE, A_SYNC_TOKEN)

        fakeRegisterOlmSessionUseCase.verifyNoInteractions()
    }
}