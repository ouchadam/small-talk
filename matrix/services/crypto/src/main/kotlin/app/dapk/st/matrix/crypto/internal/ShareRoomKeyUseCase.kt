package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.extensions.toJsonString
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.ToDevicePayload

private val ALGORITHM_OLM = AlgorithmName("m.olm.v1.curve25519-aes-sha2")
private val ALGORITHM_MEGOLM = AlgorithmName("m.megolm.v1.aes-sha2")

internal typealias ShareRoomKeyUseCase = suspend (room: Olm.RoomCryptoSession, List<Olm.DeviceCryptoSession>, RoomId) -> Unit

internal class ShareRoomKeyUseCaseImpl(
    private val credentialsStore: CredentialsStore,
    private val deviceService: DeviceService,
    private val logger: MatrixLogger,
    private val olm: Olm,
) : ShareRoomKeyUseCase {

    override suspend fun invoke(roomSessionToShare: Olm.RoomCryptoSession, olmSessionsToEncryptMessage: List<Olm.DeviceCryptoSession>, roomId: RoomId) {
        val credentials = credentialsStore.credentials()!!
        logger.crypto("creating megolm payloads for $roomId: ${olmSessionsToEncryptMessage.map { it.userId to it.deviceId }}")

        val toMessages = olmSessionsToEncryptMessage.map {
            val payload = mapOf(
                "type" to "m.room_key",
                "content" to mapOf(
                    "algorithm" to ALGORITHM_MEGOLM.value,
                    "room_id" to roomId.value,
                    "session_id" to roomSessionToShare.id.value,
                    "session_key" to roomSessionToShare.key,
                    "chain_index" to roomSessionToShare.messageIndex,
                ),
                "sender" to credentials.userId.value,
                "sender_device" to credentials.deviceId.value,
                "keys" to mapOf(
                    "ed25519" to roomSessionToShare.accountCryptoSession.fingerprint.value
                ),
                "recipient" to it.userId.value,
                "recipient_keys" to mapOf(
                    "ed25519" to it.fingerprint.value
                )
            )

            val result = with(olm) { it.encrypt(payload.toJsonString()) }
            DeviceService.ToDeviceMessage(
                senderId = it.userId,
                deviceId = it.deviceId,
                ToDevicePayload.EncryptedToDevicePayload(
                    algorithmName = ALGORITHM_OLM,
                    senderKey = roomSessionToShare.accountCryptoSession.senderKey,
                    cipherText = mapOf(
                        it.identity to ToDevicePayload.EncryptedToDevicePayload.Inner(
                            cipherText = result.cipherText,
                            type = result.type,
                        )
                    )
                ),
            )
        }
        logger.crypto("sharing keys")
        deviceService.sendRoomKeyToDevice(roomSessionToShare.id, toMessages)
    }
}