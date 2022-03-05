package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.common.crypto
import app.dapk.st.matrix.device.DeviceService

internal typealias UpdateKnownOlmSessionUseCase = suspend (List<UserId>, SyncToken?) -> Unit

internal class UpdateKnownOlmSessionUseCaseImpl(
    private val fetchAccountCryptoUseCase: FetchAccountCryptoUseCase,
    private val deviceService: DeviceService,
    private val registerOlmSessionUseCase: RegisterOlmSessionUseCase,
    private val logger: MatrixLogger,
) : UpdateKnownOlmSessionUseCase {

    override suspend fun invoke(userIds: List<UserId>, syncToken: SyncToken?) {
        logger.crypto("updating olm sessions for ${userIds.map { it.value }}")
        val account = fetchAccountCryptoUseCase.invoke()
        val keys = deviceService.fetchDevices(userIds, syncToken).filterNot { it.deviceId == account.deviceKeys.deviceId }
        if (keys.isNotEmpty()) {
            registerOlmSessionUseCase.invoke(keys, account)
        } else {
            logger.crypto("no valid devices keys found to update")
        }
    }

}