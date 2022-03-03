package app.dapk.st.matrix.crypto.internal

import fake.FakeDeviceService
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.*
import internalfake.FakeFetchAccountCryptoUseCase
import internalfake.FakeRegisterOlmSessionUseCase
import internalfake.FakeShareRoomKeyUseCase
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ROOM_ID = aRoomId()
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession()
private val A_ROOM_CRYPTO_SESSION = aRoomCryptoSession()
private val USERS_IN_ROOM = listOf(aUserId())
private val NEW_DEVICES = listOf(aDeviceKeys())
private val MISSING_OLM_SESSIONS = listOf(aDeviceCryptoSession())

class FetchMegolmSessionUseCaseTest {

    private val fakeOlm = FakeOlm()
    private val deviceService = FakeDeviceService()
    private val roomMembersProvider = FakeRoomMembersProvider()
    private val fakeRegisterOlmSessionUseCase = FakeRegisterOlmSessionUseCase()
    private val fakeShareRoomKeyUseCase = FakeShareRoomKeyUseCase()

    private val fetchMegolmSessionUseCase = FetchMegolmSessionUseCaseImpl(
        fakeOlm,
        deviceService,
        FakeFetchAccountCryptoUseCase().also { it.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION) },
        roomMembersProvider,
        fakeRegisterOlmSessionUseCase,
        fakeShareRoomKeyUseCase,
        FakeMatrixLogger(),
    )

    @Test
    fun `given new devices with missing olm sessions when fetching megolm session then creates olm session, megolm session and shares megolm key`() = runTest {
        fakeOlm.givenRoomCrypto(A_ROOM_ID, AN_ACCOUNT_CRYPTO_SESSION).returns(A_ROOM_CRYPTO_SESSION)
        roomMembersProvider.givenUserIdsForRoom(A_ROOM_ID).returns(USERS_IN_ROOM)
        deviceService.givenNewDevices(AN_ACCOUNT_CRYPTO_SESSION.deviceKeys, USERS_IN_ROOM, A_ROOM_CRYPTO_SESSION.id).returns(NEW_DEVICES)
        fakeOlm.givenMissingOlmSessions(NEW_DEVICES).returns(MISSING_OLM_SESSIONS)
        fakeRegisterOlmSessionUseCase.givenRegistersSessions(NEW_DEVICES, AN_ACCOUNT_CRYPTO_SESSION).returns(MISSING_OLM_SESSIONS)
        fakeShareRoomKeyUseCase.expect(A_ROOM_CRYPTO_SESSION, MISSING_OLM_SESSIONS, A_ROOM_ID)

        val result = fetchMegolmSessionUseCase.invoke(aRoomId())

        result shouldBeEqualTo A_ROOM_CRYPTO_SESSION
    }

    @Test
    fun `given no new devices when fetching megolm session then returns existing megolm session`() = runTest {
        fakeOlm.givenRoomCrypto(A_ROOM_ID, AN_ACCOUNT_CRYPTO_SESSION).returns(A_ROOM_CRYPTO_SESSION)
        roomMembersProvider.givenUserIdsForRoom(A_ROOM_ID).returns(USERS_IN_ROOM)
        deviceService.givenNewDevices(AN_ACCOUNT_CRYPTO_SESSION.deviceKeys, USERS_IN_ROOM, A_ROOM_CRYPTO_SESSION.id).returns(emptyList())

        val result = fetchMegolmSessionUseCase.invoke(aRoomId())

        result shouldBeEqualTo A_ROOM_CRYPTO_SESSION
    }
}