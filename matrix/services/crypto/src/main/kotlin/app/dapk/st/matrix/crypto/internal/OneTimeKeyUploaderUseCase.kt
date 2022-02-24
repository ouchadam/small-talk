package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.ServerKeyCount
import app.dapk.st.matrix.common.crypto
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService

typealias MaybeCreateAndUploadOneTimeKeysUseCase = suspend (ServerKeyCount) -> Unit

internal class MaybeCreateAndUploadOneTimeKeysUseCaseImpl(
    private val fetchAccountCryptoUseCase: FetchAccountCryptoUseCase,
    private val olm: Olm,
    private val credentialsStore: CredentialsStore,
    private val deviceService: DeviceService,
    private val logger: MatrixLogger,
): MaybeCreateAndUploadOneTimeKeysUseCase {

    override suspend fun invoke(currentServerKeyCount: ServerKeyCount) {
        val ensureCryptoAccount = fetchAccountCryptoUseCase.invoke()
        val keysDiff = (ensureCryptoAccount.maxKeys / 2) - currentServerKeyCount.value
        if (keysDiff > 0) {
            logger.crypto("current otk: $currentServerKeyCount, creating: $keysDiff")
            ensureCryptoAccount.createAndUploadOneTimeKeys(countToCreate = keysDiff + (ensureCryptoAccount.maxKeys / 4))
        } else {
            logger.crypto("current otk: $currentServerKeyCount, not creating new keys")
        }
    }

    private suspend fun Olm.AccountCryptoSession.createAndUploadOneTimeKeys(countToCreate: Int) {
        with(olm) {
            generateOneTimeKeys(countToCreate, credentialsStore.credentials()!!) {
                kotlin.runCatching {
                    deviceService.uploadOneTimeKeys(it)
                }.onFailure {
                    logger.crypto("failed to uploading OTK ${it.message}")
                }
            }
        }
    }
}