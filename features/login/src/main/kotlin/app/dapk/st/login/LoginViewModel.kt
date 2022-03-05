package app.dapk.st.login

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.logP
import app.dapk.st.login.LoginEvent.LoginComplete
import app.dapk.st.login.LoginScreenState.*
import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.push.RegisterFirebasePushTokenUseCase
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val firebasePushTokenUseCase: RegisterFirebasePushTokenUseCase,
    private val profileService: ProfileService,
    private val errorTracker: ErrorTracker,
) : DapkViewModel<LoginScreenState, LoginEvent>(
    initialState = Idle
) {

    fun login(userName: String, password: String) {
        state = Loading
        viewModelScope.launch {
            kotlin.runCatching {
                logP("login") {
                    authService.login(userName, password).also {
                        listOf(
                            async { firebasePushTokenUseCase.registerCurrentToken() },
                            async { preloadMe() },
                        ).awaitAll()
                    }
                }
            }.onFailure {
                errorTracker.track(it)
                state = Error(it)
            }.onSuccess {
                _events.tryEmit(LoginComplete)
            }
        }
    }

    private suspend fun preloadMe() = profileService.me(forceRefresh = false)

    fun start() {
        state = Idle
    }
}
