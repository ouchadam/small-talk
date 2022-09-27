package app.dapk.st.matrix.device

import app.dapk.st.matrix.InstallExtender
import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.MatrixServiceProvider
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.device.internal.ClaimKeysResponse
import app.dapk.st.matrix.device.internal.DefaultDeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val SERVICE_KEY = DeviceService::class

interface DeviceService : MatrixService {

    suspend fun uploadDeviceKeys(deviceKeys: DeviceKeys)
    suspend fun uploadOneTimeKeys(oneTimeKeys: OneTimeKeys)
    suspend fun fetchDevices(userIds: List<UserId>, syncToken: SyncToken?): List<DeviceKeys>
    suspend fun checkForNewDevices(self: DeviceKeys, userIds: List<UserId>, id: SessionId): List<DeviceKeys>
    suspend fun ensureDevice(userId: UserId, deviceId: DeviceId): DeviceKeys
    suspend fun claimKeys(claims: List<KeyClaim>): ClaimKeysResponse
    suspend fun sendRoomKeyToDevice(sessionId: SessionId, messages: List<ToDeviceMessage>)
    suspend fun sendToDevice(eventType: EventType, transactionId: String, userId: UserId, deviceId: DeviceId, payload: ToDevicePayload)
    suspend fun updateStaleDevices(userIds: List<UserId>)

    @JvmInline
    value class OneTimeKeys(val keys: List<Key>) {

        sealed interface Key {
            data class SignedCurve(val keyId: String, val value: String, val signature: Ed25519Signature) : Key {
                data class Ed25519Signature(val value: SignedJson, val deviceId: DeviceId, val userId: UserId)
            }
        }

    }

    data class KeyClaim(val userId: UserId, val deviceId: DeviceId, val algorithmName: AlgorithmName)

    data class ToDeviceMessage(
        val senderId: UserId,
        val deviceId: DeviceId,
        val encryptedMessage: ToDevicePayload.EncryptedToDevicePayload
    )
}


@Serializable
sealed class ToDevicePayload {

    @Serializable
    data class EncryptedToDevicePayload(
        @SerialName("algorithm") val algorithmName: AlgorithmName,
        @SerialName("sender_key") val senderKey: Curve25519,
        @SerialName("ciphertext") val cipherText: Map<Curve25519, Inner>,
    ) : ToDevicePayload() {

        @Serializable
        data class Inner(
            @SerialName("body") val cipherText: CipherText,
            @SerialName("type") val type: Long,
        )
    }

    @Serializable
    data class VerificationRequest(
        @SerialName("from_device") val fromDevice: DeviceId,
        @SerialName("methods") val methods: List<String>,
        @SerialName("transaction_id") val transactionId: String,
        @SerialName("timestamp") val timestampPosix: Long,
        ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationStart(
        @SerialName("from_device") val fromDevice: DeviceId,
        @SerialName("method") val method: String,
        @SerialName("key_agreement_protocols") val protocols: List<String>,
        @SerialName("hashes") val hashes: List<String>,
        @SerialName("message_authentication_codes") val codes: List<String>,
        @SerialName("short_authentication_string") val short: List<String>,
        @SerialName("transaction_id") val transactionId: String,
    ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationAccept(
        @SerialName("transaction_id") val transactionId: String,
        @SerialName("from_device") val fromDevice: DeviceId,
        @SerialName("method") val method: String,
        @SerialName("key_agreement_protocol") val protocol: String,
        @SerialName("hash") val hash: String,
        @SerialName("message_authentication_code") val code: String,
        @SerialName("short_authentication_string") val short: List<String>,
        @SerialName("commitment") val commitment: String,
    ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationReady(
        @SerialName("from_device") val fromDevice: DeviceId,
        @SerialName("methods") val methods: List<String>,
        @SerialName("transaction_id") val transactionId: String,
    ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationKey(
        @SerialName("transaction_id") val transactionId: String,
        @SerialName("key") val key: String,
    ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationMac(
        @SerialName("transaction_id") val transactionId: String,
        @SerialName("keys") val keys: String,
        @SerialName("mac") val mac: Map<String, String>,
    ) : ToDevicePayload(), VerificationPayload

    @Serializable
    data class VerificationDone(
        @SerialName("transaction_id") val transactionId: String,
    ) : ToDevicePayload(), VerificationPayload


    sealed interface VerificationPayload
}

fun MatrixServiceInstaller.installEncryptionService(knownDeviceStore: KnownDeviceStore): InstallExtender<DeviceService> {
    return this.install { (httpClient, _, _, logger) ->
        SERVICE_KEY to DefaultDeviceService(httpClient, logger, knownDeviceStore)
    }
}

fun MatrixServiceProvider.deviceService(): DeviceService = this.getService(key = SERVICE_KEY)

interface KnownDeviceStore {
    suspend fun updateDevices(devices: Map<UserId, Map<DeviceId, DeviceKeys>>): List<DeviceKeys>
    suspend fun markOutdated(userIds: List<UserId>)
    suspend fun maybeConsumeOutdated(userIds: List<UserId>): List<UserId>
    suspend fun devicesMegolmSession(userIds: List<UserId>, sessionId: SessionId): List<DeviceKeys>
    suspend fun associateSession(sessionId: SessionId, deviceIds: List<DeviceId>)
    suspend fun device(userId: UserId, deviceId: DeviceId): DeviceKeys?
}