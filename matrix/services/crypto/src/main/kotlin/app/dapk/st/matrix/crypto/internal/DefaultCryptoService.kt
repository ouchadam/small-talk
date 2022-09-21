package app.dapk.st.matrix.crypto.internal

import app.dapk.st.core.logP
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Crypto
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.crypto.ImportResult
import app.dapk.st.matrix.crypto.Verification
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

internal class DefaultCryptoService(
    private val olmCrypto: OlmCrypto,
    private val verificationHandler: VerificationHandler,
    private val roomKeyImporter: RoomKeyImporter,
    private val mediaEncrypter: MediaEncrypter,
    private val logger: MatrixLogger,
) : CryptoService {

    override suspend fun encrypt(input: InputStream): Crypto.MediaEncryptionResult {
        return mediaEncrypter.encrypt(input)
    }

    override suspend fun encrypt(roomId: RoomId, credentials: DeviceCredentials, messageJson: JsonString): Crypto.EncryptionResult {
        return olmCrypto.encryptMessage(roomId, credentials, messageJson)
    }

    override suspend fun decrypt(encryptedPayload: EncryptedMessageContent): DecryptionResult {
        return olmCrypto.decrypt(encryptedPayload).also {
            logger.matrixLog("decrypted: $it")
        }
    }

    override suspend fun importRoomKeys(keys: List<SharedRoomKey>) {
        olmCrypto.importRoomKeys(keys)
    }

    override suspend fun maybeCreateMoreKeys(serverKeyCount: ServerKeyCount) {
        olmCrypto.maybeCreateMoreKeys(serverKeyCount)
    }

    override suspend fun updateOlmSession(userIds: List<UserId>, syncToken: SyncToken?) {
        olmCrypto.updateOlmSessions(userIds, syncToken)
    }

    override suspend fun onVerificationEvent(event: Verification.Event) {
        verificationHandler.onVerificationEvent(event)
    }

    override fun verificationState(): Flow<Verification.State> {
        return verificationHandler.stateFlow
    }

    override suspend fun verificationAction(verificationAction: Verification.Action) {
        verificationHandler.onUserVerificationAction(verificationAction)
    }

    override suspend fun InputStream.importRoomKeys(password: String): Flow<ImportResult> {
        return with(roomKeyImporter) {
            importRoomKeys(password) {
                importRoomKeys(it)
            }.logP("import room keys")
        }
    }
}
