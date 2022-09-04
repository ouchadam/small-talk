package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Crypto
import app.dapk.st.matrix.crypto.Olm

internal class OlmCrypto(
    private val olm: Olm,
    private val encryptMessageWithMegolmUseCase: EncryptMessageWithMegolmUseCase,
    private val fetchAccountCryptoUseCase: FetchAccountCryptoUseCase,
    private val updateKnownOlmSessionUseCase: UpdateKnownOlmSessionUseCase,
    private val maybeCreateAndUploadOneTimeKeysUseCase: MaybeCreateAndUploadOneTimeKeysUseCase,
    private val logger: MatrixLogger
) {

    suspend fun importRoomKeys(keys: List<SharedRoomKey>) {
        olm.import(keys)
    }

    suspend fun decrypt(payload: EncryptedMessageContent) = when (payload) {
        is EncryptedMessageContent.MegOlmV1 -> olm.decryptMegOlm(payload.sessionId, payload.cipherText)
        is EncryptedMessageContent.OlmV1 -> decryptOlm(payload)
    }

    private suspend fun decryptOlm(payload: EncryptedMessageContent.OlmV1): DecryptionResult {
        logger.crypto("decrypt olm: $payload")
        val account = fetchAccountCryptoUseCase.invoke()
        return payload.cipherFor(account)?.let { olm.decryptOlm(account, payload.senderKey, it.type, it.body) }
            ?: DecryptionResult.Failed("Missing cipher for sender : ${account.senderKey}")
    }

    suspend fun encryptMessage(roomId: RoomId, credentials: DeviceCredentials, messageJson: JsonString): Crypto.EncryptionResult {
        return encryptMessageWithMegolmUseCase.invoke(credentials, MessageToEncrypt(roomId, messageJson))
    }

    suspend fun updateOlmSessions(userId: List<UserId>, syncToken: SyncToken?) {
        updateKnownOlmSessionUseCase.invoke(userId, syncToken)
    }

    suspend fun maybeCreateMoreKeys(currentServerKeyCount: ServerKeyCount) {
        maybeCreateAndUploadOneTimeKeysUseCase.invoke(currentServerKeyCount)
    }
}

private fun EncryptedMessageContent.OlmV1.cipherFor(account: Olm.AccountCryptoSession) = this.cipherText[account.senderKey]
