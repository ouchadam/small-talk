package app.dapk.st.verification

import androidx.lifecycle.viewModelScope
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.launch

class VerificationViewModel(
    private val cryptoService: CryptoService,
) : DapkViewModel<VerificationScreenState, VerificationEvent>(
    initialState = VerificationScreenState(foo = "")
) {
    fun inSecureAccept() {
        viewModelScope.launch {
            cryptoService.verificationAction(Verification.Action.InsecureAccept)
        }
    }


}
