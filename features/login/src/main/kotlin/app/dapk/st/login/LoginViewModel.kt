package app.dapk.st.login

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.logP
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.st.login.LoginEvent.LoginComplete
import app.dapk.st.login.LoginScreenState.*
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LoginViewModel(
    private val chatEngine: ChatEngine,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val errorTracker: ErrorTracker,
) : DapkViewModel<LoginScreenState, LoginEvent>(
    initialState = Content(showServerUrl = false)
) {

    private var previousState: LoginScreenState? = null

    fun login(userName: String, password: String, serverUrl: String?) {
        state = Loading
        viewModelScope.launch {
            logP("login") {
                when (val result = chatEngine.login(LoginRequest(userName, password, serverUrl.takeIfNotEmpty()))) {
                    is LoginResult.Success -> {
                        runCatching {
                            listOf(
                                async { pushTokenRegistrar.registerCurrentToken() },
                                async { preloadMe() },
                            ).awaitAll()
                        }
                        _events.tryEmit(LoginComplete)
                    }

                    is LoginResult.Error -> {
                        errorTracker.track(result.cause)
                        state = Error(result.cause)
                    }

                    LoginResult.MissingWellKnown -> {
                        _events.tryEmit(LoginEvent.WellKnownMissing)
                        state = Content(showServerUrl = true)
                    }
                }
            }
        }
    }

    private suspend fun preloadMe() = chatEngine.me(forceRefresh = false)

    fun start() {
        val showServerUrl = previousState?.let { it is Content && it.showServerUrl } ?: false
        state = Content(showServerUrl = showServerUrl)
    }
}


private fun String?.takeIfNotEmpty() = this?.takeIf { it.isNotEmpty() }