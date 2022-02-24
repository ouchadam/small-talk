package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.crypto
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.RoomMembersProvider
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys

internal typealias FetchMegolmSessionUseCase = suspend (RoomId) -> Olm.RoomCryptoSession

internal class FetchMegolmSessionUseCaseImpl(
    private val olm: Olm,
    private val deviceService: DeviceService,
    private val fetchAccountCryptoUseCase: FetchAccountCryptoUseCase,
    private val roomMembersProvider: RoomMembersProvider,
    private val registerOlmSessionUseCase: RegisterOlmSessionUseCase,
    private val shareRoomKeyUseCase: ShareRoomKeyUseCase,
    private val logger: MatrixLogger,
) : FetchMegolmSessionUseCase {

    override suspend fun invoke(roomId: RoomId): Olm.RoomCryptoSession {
        logger.crypto("ensureOutboundMegolmSession")
        val accountCryptoSession = fetchAccountCryptoUseCase.invoke()
        return olm.ensureRoomCrypto(roomId, accountCryptoSession).also { it.maybeUpdateWithNewDevices(roomId, accountCryptoSession) }
    }

    private suspend fun Olm.RoomCryptoSession.maybeUpdateWithNewDevices(roomId: RoomId, accountCryptoSession: Olm.AccountCryptoSession) {
        val roomMemberIds = roomMembersProvider.userIdsForRoom(roomId)
        val newDevices = deviceService.checkForNewDevices(accountCryptoSession.deviceKeys, roomMemberIds, this.id)
        if (newDevices.isNotEmpty()) {
            logger.crypto("found devices to update with megolm session")
            val olmSessions = ensureOlmSessions(newDevices, accountCryptoSession)
            shareRoomKeyUseCase.invoke(this, olmSessions, roomId)
        } else {
            logger.crypto("no devices to update with megolm")
        }
    }

    private suspend fun ensureOlmSessions(newDevices: List<DeviceKeys>, accountCryptoSession: Olm.AccountCryptoSession): List<Olm.DeviceCryptoSession> {
        return olm.olmSessions(newDevices, onMissing = {
            logger.crypto("found missing olm sessions when creating megolm session ${it.map { "${it.userId}:${it.deviceId}" }}")
            registerOlmSessionUseCase.invoke(it, accountCryptoSession)
        })
    }

}