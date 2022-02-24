package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Crypto
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.*
import internalfake.FakeFetchMegolmSessionUseCase
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ROOM_ID = aRoomId()
private val A_MESSAGE_TO_ENCRYPT = aMessageToEncrypt(roomId = A_ROOM_ID)
private val AN_ENCRYPTION_CIPHER_RESULT = aCipherText()
private val A_DEVICE_CREDENTIALS = aDeviceCredentials()
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession()
private val A_ROOM_CRYPTO_SESSION = aRoomCryptoSession(accountCryptoSession = AN_ACCOUNT_CRYPTO_SESSION)

class EncryptMegolmUseCaseTest {

    private val fetchMegolmSessionUseCase = FakeFetchMegolmSessionUseCase()
    private val fakeOlm = FakeOlm()

    private val encryptMegolmUseCase = EncryptMessageWithMegolmUseCaseImpl(
        fakeOlm,
        fetchMegolmSessionUseCase,
        FakeMatrixLogger(),
    )

    @Test
    fun `given a room crypto session then encrypts messages with megolm`() = runTest {
        fetchMegolmSessionUseCase.givenSessionForRoom(A_ROOM_ID, A_ROOM_CRYPTO_SESSION)
        fakeOlm.givenEncrypts(A_ROOM_CRYPTO_SESSION, A_MESSAGE_TO_ENCRYPT.roomId, A_MESSAGE_TO_ENCRYPT.json, AN_ENCRYPTION_CIPHER_RESULT)

        val result = encryptMegolmUseCase.invoke(aDeviceCredentials(), A_MESSAGE_TO_ENCRYPT)

        result shouldBeEqualTo anEncryptionResult(
            AlgorithmName("m.megolm.v1.aes-sha2"),
            senderKey = AN_ACCOUNT_CRYPTO_SESSION.senderKey.value,
            cipherText = AN_ENCRYPTION_CIPHER_RESULT,
            sessionId = A_ROOM_CRYPTO_SESSION.id,
            deviceId = A_DEVICE_CREDENTIALS.deviceId
        )
    }
}

fun aMessageToEncrypt(
    roomId: RoomId = aRoomId(),
    messageJson: JsonString = aJsonString()
) = MessageToEncrypt(roomId, messageJson)

fun anEncryptionResult(
    algorithmName: AlgorithmName = anAlgorithmName(),
    senderKey: String = "a-sender-key",
    cipherText: CipherText = aCipherText(),
    sessionId: SessionId = aSessionId(),
    deviceId: DeviceId = aDeviceId(),
) = Crypto.EncryptionResult(algorithmName, senderKey, cipherText, sessionId, deviceId)