package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService

internal typealias FetchAccountCryptoUseCase = suspend () -> Olm.AccountCryptoSession

internal class FetchAccountCryptoUseCaseImpl(
    private val credentialsStore: CredentialsStore,
    private val olm: Olm,
    private val deviceService: DeviceService
) : FetchAccountCryptoUseCase {

    override suspend fun invoke(): Olm.AccountCryptoSession {
        val credentials = credentialsStore.credentials()!!
        return olm.ensureAccountCrypto(credentials) { accountCryptoSession ->
            deviceService.uploadDeviceKeys(accountCryptoSession.deviceKeys)
        }
    }
}