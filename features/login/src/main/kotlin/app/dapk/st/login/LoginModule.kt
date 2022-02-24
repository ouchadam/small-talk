package app.dapk.st.login

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.crypto.CryptoService
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.push.PushModule

class LoginModule(
    private val authService: AuthService,
    private val pushModule: PushModule,
    private val profileService: ProfileService,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun loginViewModel(): LoginViewModel {
        return LoginViewModel(authService, pushModule.registerFirebasePushTokenUseCase(), profileService, errorTracker)
    }
}