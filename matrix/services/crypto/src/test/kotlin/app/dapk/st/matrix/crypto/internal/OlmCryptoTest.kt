package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import fake.FakeMatrixLogger
import fake.FakeOlm
import fixture.*
import internalfake.FakeEncryptMessageWithMegolmUseCase
import internalfake.FakeFetchAccountCryptoUseCase
import internalfake.FakeMaybeCreateAndUploadOneTimeKeysUseCase
import internalfake.FakeUpdateKnownOlmSessionUseCase
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.runExpectTest

private val A_LIST_OF_SHARED_ROOM_KEYS = listOf(aSharedRoomKey())
private val A_DEVICE_CREDENTIALS = aDeviceCredentials()
private val A_ROOM_ID = aRoomId()
private val A_MESSAGE_JSON_TO_ENCRYPT = aJsonString("message!")
private val AN_EXPECTED_MESSAGE_TO_ENCRYPT = aMessageToEncrypt(A_ROOM_ID, A_MESSAGE_JSON_TO_ENCRYPT)
private val AN_ENCRYPTION_RESULT = anEncryptionResult()
private val A_LIST_OF_USER_IDS_TO_UPDATE = listOf(aUserId())
private val A_SYNC_TOKEN = aSyncToken()
private val A_SERVER_KEY_COUNT = ServerKeyCount(100)
private val A_MEGOLM_PAYLOAD = anEncryptedMegOlmV1Message()
private val AN_ACCOUNT_CRYPTO_SESSION = anAccountCryptoSession()
private val AN_OLM_PAYLOAD = anEncryptedOlmV1Message(cipherText = mapOf(AN_ACCOUNT_CRYPTO_SESSION.senderKey to aCipherTextInfo()))
private val A_DECRYPTION_RESULT = aDecryptionSuccessResult()

internal class OlmCryptoTest {

    private val fakeOlm = FakeOlm()
    private val fakeEncryptMessageWithMegolmUseCase = FakeEncryptMessageWithMegolmUseCase()
    private val fakeFetchAccountCryptoUseCase = FakeFetchAccountCryptoUseCase()
    private val fakeUpdateKnownOlmSessionUseCase = FakeUpdateKnownOlmSessionUseCase()
    private val fakeMaybeCreateAndUploadOneTimeKeysUseCase = FakeMaybeCreateAndUploadOneTimeKeysUseCase()

    private val olmCrypto = OlmCrypto(
        fakeOlm,
        fakeEncryptMessageWithMegolmUseCase,
        fakeFetchAccountCryptoUseCase,
        fakeUpdateKnownOlmSessionUseCase,
        fakeMaybeCreateAndUploadOneTimeKeysUseCase,
        FakeMatrixLogger()
    )

    @Test
    fun `when importing room keys, then delegates to olm`() = runExpectTest {
        fakeOlm.expectUnit { it.import(A_LIST_OF_SHARED_ROOM_KEYS) }

        olmCrypto.importRoomKeys(A_LIST_OF_SHARED_ROOM_KEYS)

        verifyExpects()
    }

    @Test
    fun `when encrypting message, then delegates to megolm`() = runTest {
        fakeEncryptMessageWithMegolmUseCase.givenEncrypt(A_DEVICE_CREDENTIALS, AN_EXPECTED_MESSAGE_TO_ENCRYPT).returns(AN_ENCRYPTION_RESULT)

        val result = olmCrypto.encryptMessage(A_ROOM_ID, A_DEVICE_CREDENTIALS, A_MESSAGE_JSON_TO_ENCRYPT)

        result shouldBeEqualTo AN_ENCRYPTION_RESULT
    }

    @Test
    fun `when updating olm sessions, then delegates to use case`() = runExpectTest {
        fakeUpdateKnownOlmSessionUseCase.expectUnit { it.invoke(A_LIST_OF_USER_IDS_TO_UPDATE, A_SYNC_TOKEN) }

        olmCrypto.updateOlmSessions(A_LIST_OF_USER_IDS_TO_UPDATE, A_SYNC_TOKEN)

        verifyExpects()
    }

    @Test
    fun `when maybe creating more keys, then delegates to use case`() = runExpectTest {
        fakeMaybeCreateAndUploadOneTimeKeysUseCase.expectUnit { it.invoke(A_SERVER_KEY_COUNT) }

        olmCrypto.maybeCreateMoreKeys(A_SERVER_KEY_COUNT)

        verifyExpects()
    }

    @Test
    fun `given megolm payload, when decrypting, then delegates to olm`() = runTest {
        fakeOlm.givenDecrypting(A_MEGOLM_PAYLOAD).returns(A_DECRYPTION_RESULT)

        val result = olmCrypto.decrypt(A_MEGOLM_PAYLOAD)

        result shouldBeEqualTo A_DECRYPTION_RESULT
    }

    @Test
    fun `given olm payload, when decrypting, then delegates to olm`() = runTest {
        fakeFetchAccountCryptoUseCase.givenFetch().returns(AN_ACCOUNT_CRYPTO_SESSION)
        fakeOlm.givenDecrypting(AN_OLM_PAYLOAD, AN_ACCOUNT_CRYPTO_SESSION).returns(A_DECRYPTION_RESULT)

        val result = olmCrypto.decrypt(AN_OLM_PAYLOAD)

        result shouldBeEqualTo A_DECRYPTION_RESULT
    }
}
