package app.dapk.st.matrix.crypto

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys

interface Olm {

    companion object {
        val ALGORITHM_MEGOLM = AlgorithmName("m.megolm.v1.aes-sha2")
        val ALGORITHM_OLM = AlgorithmName("m.olm.v1.curve25519-aes-sha2")
    }

    suspend fun ensureAccountCrypto(deviceCredentials: DeviceCredentials, onCreate: suspend (AccountCryptoSession) -> Unit): AccountCryptoSession
    suspend fun ensureRoomCrypto(roomId: RoomId, accountSession: AccountCryptoSession): RoomCryptoSession
    suspend fun ensureDeviceCrypto(input: OlmSessionInput, olmAccount: AccountCryptoSession): DeviceCryptoSession
    suspend fun import(keys: List<SharedRoomKey>)

    suspend fun DeviceCryptoSession.encrypt(messageJson: JsonString): EncryptionResult
    suspend fun RoomCryptoSession.encrypt(roomId: RoomId, messageJson: JsonString): CipherText
    suspend fun AccountCryptoSession.generateOneTimeKeys(
        count: Int,
        credentials: DeviceCredentials,
        publishKeys: suspend (DeviceService.OneTimeKeys) -> Unit
    )

    suspend fun decryptOlm(olmAccount: AccountCryptoSession, senderKey: Curve25519, type: Int, body: CipherText): DecryptionResult
    suspend fun decryptMegOlm(sessionId: SessionId, cipherText: CipherText): DecryptionResult
    suspend fun verifyExternalUser(keys: Ed25519?, recipeientKeys: Ed25519?): Boolean
    suspend fun olmSessions(devices: List<DeviceKeys>, onMissing: suspend (List<DeviceKeys>) -> List<DeviceCryptoSession>): List<DeviceCryptoSession>
    suspend fun sasSession(deviceCredentials: DeviceCredentials): SasSession

    interface SasSession {
        suspend fun generateCommitment(hash: String, startJsonString: String): String
        suspend fun calculateMac(
            selfUserId: UserId,
            selfDeviceId: DeviceId,
            otherUserId: UserId,
            otherDeviceId: DeviceId,
            transactionId: String
        ): MacResult

        fun release()
        fun publicKey(): String
        fun setTheirPublicKey(key: String)
    }

    data class MacResult(val mac: Map<String, String>, val keys: String)

    data class EncryptionResult(
        val cipherText: CipherText,
        val type: Long,
    )

    data class OlmSessionInput(
        val oneTimeKey: String,
        val identity: Curve25519,
        val deviceId: DeviceId,
        val userId: UserId,
        val fingerprint: Ed25519,
    )

    data class DeviceCryptoSession(
        val deviceId: DeviceId,
        val userId: UserId,
        val identity: Curve25519,
        val fingerprint: Ed25519,
        val olmSession: List<Any>,
    )

    data class AccountCryptoSession(
        val fingerprint: Ed25519,
        val senderKey: Curve25519,
        val deviceKeys: DeviceKeys,
        val hasKeys: Boolean,
        val maxKeys: Int,
        val olmAccount: Any,
    )

    data class RoomCryptoSession(
        val creationTimestampUtc: Long,
        val key: String,
        val messageIndex: Int,
        val accountCryptoSession: AccountCryptoSession,
        val id: SessionId,
        val outBound: Any,
    )

}