package app.dapk.st.matrix.sync.internal.room

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.DeviceNotifier
import app.dapk.st.matrix.sync.KeySharer
import app.dapk.st.matrix.sync.MaybeCreateMoreKeys
import app.dapk.st.matrix.sync.VerificationHandler
import app.dapk.st.matrix.sync.internal.request.ApiSyncResponse
import app.dapk.st.matrix.sync.internal.request.ApiToDeviceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class SyncSideEffects(
    private val keySharer: KeySharer,
    private val verificationHandler: VerificationHandler,
    private val notifyDevicesUpdated: DeviceNotifier,
    private val messageDecrypter: MessageDecrypter,
    private val json: Json,
    private val oneTimeKeyProducer: MaybeCreateMoreKeys,
    private val logger: MatrixLogger,
) {

    suspend fun blockingSideEffects(userId: UserId, response: ApiSyncResponse, requestToken: SyncToken?): SideEffectResult {
        return withContext(Dispatchers.IO) {
            logger.matrixLog("process side effects")
            response.deviceLists?.changed?.ifEmpty { null }?.let {
                notifyDevicesUpdated.notifyChanges(it, requestToken)
            }

            oneTimeKeyProducer.onServerKeyCount(response.oneTimeKeysCount?.get("signed_curve25519") ?: ServerKeyCount(0))

            val decryptedToDeviceEvents = decryptedToDeviceEvents(response)
            val roomKeys = handleRoomKeyShares(decryptedToDeviceEvents)

            checkForVerificationRequests(userId, decryptedToDeviceEvents)
            SideEffectResult(roomKeys?.map { it.roomId } ?: emptyList())
        }
    }

    private suspend fun checkForVerificationRequests(selfId: UserId, toDeviceEvents: List<ApiToDeviceEvent>?) {
        toDeviceEvents?.filterIsInstance<ApiToDeviceEvent.ApiVerificationEvent>()
            ?.ifEmpty { null }
            ?.also {
                if (it.size > 1) {
                    logger.matrixLog(MatrixLogTag.VERIFICATION, "found more verification events than expected, using first")
                }
                verificationHandler.handle(it.first())
            }
    }

    private suspend fun handleRoomKeyShares(toDeviceEvents: List<ApiToDeviceEvent>?): List<SharedRoomKey>? {
        return toDeviceEvents?.filterIsInstance<ApiToDeviceEvent.RoomKey>()?.map {
            SharedRoomKey(
                it.content.algorithmName,
                it.content.roomId,
                it.content.sessionId,
                it.content.sessionKey,
                isExported = false
            )
        }?.also { keySharer.share(it) }
    }

    private suspend fun decryptedToDeviceEvents(response: ApiSyncResponse) = response.toDevice?.events
        ?.mapNotNull {
            when (it) {
                is ApiToDeviceEvent.Encrypted -> decryptEncryptedToDevice(it)
                else -> it
            }
        }

    private suspend fun decryptEncryptedToDevice(it: ApiToDeviceEvent.Encrypted): ApiToDeviceEvent? {
        logger.matrixLog("got encrypted toDevice event: from ${it.senderId}: $")
        return it.content.export(it.senderId)?.let {
            messageDecrypter.decrypt(it).let {
                when (it) {
                    is DecryptionResult.Failed -> null
                    is DecryptionResult.Success -> json.decodeFromString(ApiToDeviceEvent.serializer(), it.payload.value)
                }
            }
        }
    }
}

data class SideEffectResult(val roomsWithNewKeys: List<RoomId>)