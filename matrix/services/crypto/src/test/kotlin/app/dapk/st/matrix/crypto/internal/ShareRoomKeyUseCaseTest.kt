package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.SessionId
import app.dapk.st.matrix.common.extensions.toJsonString
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.ToDevicePayload
import fake.FakeCredentialsStore
import fake.FakeDeviceService
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.*
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.expect

private val A_USER_CREDENTIALS = aUserCredentials()
private val A_ROOM_CRYPTO_SESSION = aRoomCryptoSession()
private val A_ROOM_ID = aRoomId()
private val ALGORITHM_MEGOLM = AlgorithmName("m.megolm.v1.aes-sha2")
private val ALGORITHM_OLM = AlgorithmName("m.olm.v1.curve25519-aes-sha2")
private val AN_OLM_ENCRPYTION_RESULT = Olm.EncryptionResult(aCipherText(), type = 1)

class ShareRoomKeyUseCaseTest {

    private val fakeDeviceService = FakeDeviceService()
    private val fakeOlm = FakeOlm()

    private val shareRoomKeyUseCase = ShareRoomKeyUseCaseImpl(
        FakeCredentialsStore().also { it.givenCredentials().returns(A_USER_CREDENTIALS) },
        fakeDeviceService,
        FakeMatrixLogger(),
        fakeOlm
    )

    @Test
    fun `when sharing room key then encrypts with olm session and sends to device`() = runTest {
        fakeDeviceService.expect { it.sendRoomKeyToDevice(SessionId(any()), any()) }
        val olmSessionToEncryptWith = aDeviceCryptoSession()
        fakeOlm.givenEncrypts(olmSessionToEncryptWith, expectedPayload(olmSessionToEncryptWith)).returns(AN_OLM_ENCRPYTION_RESULT)

        shareRoomKeyUseCase.invoke(A_ROOM_CRYPTO_SESSION, listOf(olmSessionToEncryptWith), A_ROOM_ID)

        coVerify {
            fakeDeviceService.sendRoomKeyToDevice(A_ROOM_CRYPTO_SESSION.id, listOf(expectedToDeviceRoomShareMessage(olmSessionToEncryptWith)))
        }
    }

    private fun expectedToDeviceRoomShareMessage(olmSessionToEncryptWith: Olm.DeviceCryptoSession) = DeviceService.ToDeviceMessage(
        olmSessionToEncryptWith.userId,
        olmSessionToEncryptWith.deviceId,
        ToDevicePayload.EncryptedToDevicePayload(
            algorithmName = ALGORITHM_OLM,
            senderKey = A_ROOM_CRYPTO_SESSION.accountCryptoSession.senderKey,
            cipherText = mapOf(
                olmSessionToEncryptWith.identity to ToDevicePayload.EncryptedToDevicePayload.Inner(
                    cipherText = AN_OLM_ENCRPYTION_RESULT.cipherText,
                    type = AN_OLM_ENCRPYTION_RESULT.type,
                )
            )
        )
    )

    private fun expectedPayload(deviceCryptoSession: Olm.DeviceCryptoSession) = mapOf(
        "type" to "m.room_key",
        "content" to mapOf(
            "algorithm" to ALGORITHM_MEGOLM.value,
            "room_id" to A_ROOM_ID.value,
            "session_id" to A_ROOM_CRYPTO_SESSION.id.value,
            "session_key" to A_ROOM_CRYPTO_SESSION.key,
            "chain_index" to A_ROOM_CRYPTO_SESSION.messageIndex,
        ),
        "sender" to A_USER_CREDENTIALS.userId.value,
        "sender_device" to A_USER_CREDENTIALS.deviceId.value,
        "keys" to mapOf(
            "ed25519" to A_ROOM_CRYPTO_SESSION.accountCryptoSession.fingerprint.value
        ),
        "recipient" to deviceCryptoSession.userId.value,
        "recipient_keys" to mapOf(
            "ed25519" to deviceCryptoSession.fingerprint.value
        )
    ).toJsonString()
}