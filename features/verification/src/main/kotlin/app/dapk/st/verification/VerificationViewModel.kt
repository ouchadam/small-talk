package app.dapk.st.verification

import app.dapk.st.engine.ChatEngine
import app.dapk.st.viewmodel.DapkViewModel

class VerificationViewModel(
    private val chatEngine: ChatEngine,
) : DapkViewModel<VerificationScreenState, VerificationEvent>(
    initialState = VerificationScreenState(foo = "")
) {
    fun inSecureAccept() {
        // TODO verify via chat-engine
//        viewModelScope.launch {
//            cryptoService.verificationAction(Verification.Action.InsecureAccept)
//        }
    }


}
