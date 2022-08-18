package app.dapk.st.login

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.logP
import app.dapk.st.login.LoginEvent.LoginComplete
import app.dapk.st.login.LoginScreenState.*
import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val profileService: ProfileService,
    private val errorTracker: ErrorTracker,
) : DapkViewModel<LoginScreenState, LoginEvent>(
    initialState = Content(showServerUrl = false)
) {

    private var previousState: LoginScreenState? = null

    fun login(userName: String, password: String, serverUrl: String?) {
        state = Loading
        viewModelScope.launch {
            logP("login") {
                when (val result = authService.login(AuthService.LoginRequest(userName, password, serverUrl.takeIfNotEmpty()))) {
                    is AuthService.LoginResult.Success -> {
                        runCatching {
                            listOf(
                                async { pushTokenRegistrar.registerCurrentToken() },
                                async { preloadMe() },
                            ).awaitAll()
                        }
                        _events.tryEmit(LoginComplete)
                    }
                    is AuthService.LoginResult.Error -> {
                        errorTracker.track(result.cause)
                        state = Error(result.cause)
                    }
                    AuthService.LoginResult.MissingWellKnown -> {
                        _events.tryEmit(LoginEvent.WellKnownMissing)
                        state = Content(showServerUrl = true)
                    }
                }
            }
        }
    }

    private suspend fun preloadMe() = profileService.me(forceRefresh = false)

    fun start() {
        val showServerUrl = previousState?.let { it is Content && it.showServerUrl } ?: false
        state = Content(showServerUrl = showServerUrl)
    }
}


private fun String?.takeIfNotEmpty() = this?.takeIf { it.isNotEmpty() }