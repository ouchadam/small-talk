package app.dapk.st.verification

import app.dapk.st.core.ProvidableModule
import app.dapk.st.matrix.crypto.CryptoService

class VerificationModule(
    private val cryptoService: CryptoService
) : ProvidableModule {

    fun verificationViewModel(): VerificationViewModel {
        return VerificationViewModel(cryptoService)
    }

}