package app.dapk.st.matrix.device.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.DeviceService.OneTimeKeys.Key.SignedCurve.Ed25519Signature
import app.dapk.st.matrix.device.KnownDeviceStore
import app.dapk.st.matrix.device.ToDevicePayload
import app.dapk.st.matrix.http.MatrixHttpClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.*

internal class DefaultDeviceService(
    private val httpClient: MatrixHttpClient,
    private val logger: MatrixLogger,
    private val knownDeviceStore: KnownDeviceStore,
) : DeviceService {

    override suspend fun uploadOneTimeKeys(oneTimeKeys: DeviceService.OneTimeKeys) {
        val jsonCryptoKeys = oneTimeKeys.keys.associate {
            when (it) {
                is DeviceService.OneTimeKeys.Key.SignedCurve -> {
                    "signed_curve25519:${it.keyId}" to JsonObject(
                        content = mapOf(
                            "key" to JsonPrimitive(it.value),
                            "signatures" to it.signature.toJson()
                        )
                    )
                }
            }
        }

        val keyRequest = UploadKeyRequest(
            deviceKeys = null,
            oneTimeKeys = jsonCryptoKeys
        )
        logger.matrixLog("uploading one time keys")
        logger.matrixLog(jsonCryptoKeys)
        httpClient.execute(uploadKeysRequest(keyRequest)).also {
            logger.matrixLog(it)
        }
    }

    override suspend fun uploadDeviceKeys(deviceKeys: DeviceKeys) {
        logger.matrixLog("uploading device keys")
        val keyRequest = UploadKeyRequest(
            deviceKeys = deviceKeys,
            oneTimeKeys = null
        )
        logger.matrixLog(keyRequest)
        httpClient.execute(uploadKeysRequest(keyRequest)).also {
            logger.matrixLog(it)
        }
    }

    private fun Ed25519Signature.toJson() = JsonObject(
        content = mapOf(
            this.userId.value to JsonObject(
                content = mapOf(
                    "ed25519:${this.deviceId.value}" to JsonPrimitive(this.value.value)
                )
            )
        )
    )

    override suspend fun fetchDevices(userIds: List<UserId>, syncToken: SyncToken?): List<DeviceKeys> {
        val request = QueryKeysRequest(
            deviceKeys = userIds.associateWith { emptyList() },
            token = syncToken?.value,
        )

        logger.crypto("querying keys for: $userIds")
        val apiResponse = httpClient.execute(queryKeys(request))
        logger.crypto("got keys for ${apiResponse.deviceKeys.keys}")

        return apiResponse.deviceKeys.values.map { it.values }.flatten().also {
            knownDeviceStore.updateDevices(apiResponse.deviceKeys)
        }
    }

    override suspend fun claimKeys(claims: List<DeviceService.KeyClaim>): ClaimKeysResponse {
        val request = ClaimKeysRequest(oneTimeKeys = claims.groupBy { it.userId }.mapValues {
            it.value.associate { it.deviceId to it.algorithmName }
        })
        return httpClient.execute(claimKeys(request))
    }

    override suspend fun sendRoomKeyToDevice(sessionId: SessionId, messages: List<DeviceService.ToDeviceMessage>) {
        val associateBy = messages.groupBy { it.senderId }.mapValues {
            it.value.associateBy { it.deviceId }.mapValues { it.value.encryptedMessage }
        }

        logger.crypto("sending to device: ${associateBy.map { it.key to it.value.keys }}")

        val txnId = UUID.randomUUID().toString()
        httpClient.execute(sendToDeviceRequest(EventType.ENCRYPTED, txnId, SendToDeviceRequest(associateBy)))
        knownDeviceStore.associateSession(sessionId, messages.map { it.deviceId })
    }

    override suspend fun sendToDevice(eventType: EventType, transactionId: String, userId: UserId, deviceId: DeviceId, payload: ToDevicePayload) {
        val messages = mapOf(
            userId to mapOf(
                deviceId to payload
            )
        )
        httpClient.execute(sendToDeviceRequest(eventType, transactionId, SendToDeviceRequest(messages)))
    }

    override suspend fun updateStaleDevices(userIds: List<UserId>) {
        logger.matrixLog("devices changed: $userIds")
        knownDeviceStore.markOutdated(userIds)
    }

    override suspend fun checkForNewDevices(self: DeviceKeys, userIds: List<UserId>, id: SessionId): List<DeviceKeys> {
        val outdatedUsersToNotify = knownDeviceStore.maybeConsumeOutdated(userIds)
        logger.crypto("found outdated users: $outdatedUsersToNotify")
        val notOutdatedIds = userIds.filterNot { outdatedUsersToNotify.contains(it) }
        val knownKeys = knownDeviceStore.devicesMegolmSession(notOutdatedIds, id)

        val knownUsers = knownKeys.map { it.userId }
        val usersWithoutKnownSessions = notOutdatedIds - knownUsers.toSet()
        logger.crypto("found users without known sessions: $usersWithoutKnownSessions")

        val usersToUpdate = outdatedUsersToNotify + usersWithoutKnownSessions
        val newDevices = if (usersToUpdate.isNotEmpty()) {
            fetchDevices(usersToUpdate, syncToken = null).filter {
                it.deviceId != self.deviceId
            }
        } else {
            logger.crypto("didn't find any new devices")
            emptyList()
        }

        return newDevices
    }

    override suspend fun ensureDevice(userId: UserId, deviceId: DeviceId): DeviceKeys {
        return knownDeviceStore.device(userId, deviceId) ?: fetchDevices(listOf(userId), syncToken = null).first()
    }
}

