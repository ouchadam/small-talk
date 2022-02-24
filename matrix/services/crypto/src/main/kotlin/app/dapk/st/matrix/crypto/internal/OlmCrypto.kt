package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Crypto
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService

internal class OlmCrypto(
    private val olm: Olm,
    private val deviceService: DeviceService,
    private val logger: MatrixLogger,
    private val registerOlmSessionUseCase: RegisterOlmSessionUseCase,
    private val encryptMessageWithMegolmUseCase: EncryptMessageWithMegolmUseCase,
    private val fetchAccountCryptoUseCase: FetchAccountCryptoUseCase,
    private val maybeCreateAndUploadOneTimeKeysUseCase: MaybeCreateAndUploadOneTimeKeysUseCase
) {

    suspend fun importRoomKeys(keys: List<SharedRoomKey>) {
        logger.crypto("import room keys : ${keys.size}")
        olm.import(keys)
    }

    suspend fun decrypt(payload: EncryptedMessageContent): DecryptionResult {
        return when (payload) {
            is EncryptedMessageContent.MegOlmV1 -> {
                olm.decryptMegOlm(payload.sessionId, payload.cipherText)
            }
            is EncryptedMessageContent.OlmV1 -> {
                val account = fetchAccountCryptoUseCase.invoke()
                logger.crypto("decrypt olm: $payload")
                payload.cipherText[account.senderKey]?.let {
                    olm.decryptOlm(account, payload.senderKey, it.type.toLong(), it.body)
                } ?: DecryptionResult.Failed("Missing cipher for sender : ${account.senderKey}")
            }
        }
    }

    suspend fun encryptMessage(roomId: RoomId, credentials: DeviceCredentials, messageJson: JsonString): Crypto.EncryptionResult {
        val messageToEncrypt = MessageToEncrypt(roomId, messageJson)
        return encryptMessageWithMegolmUseCase.invoke(credentials, messageToEncrypt)
    }

    suspend fun updateOlmSessions(userId: List<UserId>, syncToken: SyncToken?) {
        logger.crypto("updating olm sessions for ${userId.map { it.value }}")
        val account = fetchAccountCryptoUseCase.invoke()
        val keys = deviceService.fetchDevices(userId, syncToken).filterNot { it.deviceId == account.deviceKeys.deviceId }
        registerOlmSessionUseCase.invoke(keys, account)
    }

    suspend fun maybeCreateMoreKeys(currentServerKeyCount: ServerKeyCount) {
        maybeCreateAndUploadOneTimeKeysUseCase.invoke(currentServerKeyCount)
    }
}
