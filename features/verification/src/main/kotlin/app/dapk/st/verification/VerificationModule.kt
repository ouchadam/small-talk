package app.dapk.st.verification

import app.dapk.st.core.ProvidableModule
import app.dapk.st.engine.ChatEngine

class VerificationModule(
    private val chatEngine: ChatEngine,
) : ProvidableModule {

    fun verificationViewModel(): VerificationViewModel {
        return VerificationViewModel(chatEngine)
    }

}