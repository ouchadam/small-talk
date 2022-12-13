package app.dapk.st.login.state

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.logP
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.st.push.PushTokenRegistrar
import app.dapk.state.async
import app.dapk.state.change
import app.dapk.state.createReducer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

fun loginReducer(
    chatEngine: ChatEngine,
    pushTokenRegistrar: PushTokenRegistrar,
    errorTracker: ErrorTracker,
    eventEmitter: suspend (LoginEvent) -> Unit,
) = createReducer(
    initialState = LoginScreenState(showServerUrl = false, content = LoginScreenState.Content.Idle),

    change(LoginAction.ComponentLifecycle.Visible::class) { _, state ->
        LoginScreenState(state.showServerUrl, content = LoginScreenState.Content.Idle)
    },

    change(LoginAction.UpdateContent::class) { action, state ->
        state.copy(content = action.content)
    },

    change(LoginAction.UpdateState::class) { action, _ ->
        action.state
    },

    async(LoginAction.Login::class) { action ->
        coroutineScope.launch {
            logP("login") {
                dispatch(LoginAction.UpdateContent(LoginScreenState.Content.Loading))
                val request = LoginRequest(action.userName, action.password, action.serverUrl.takeIfNotEmpty())
                when (val result = chatEngine.login(request)) {
                    is LoginResult.Success -> {
                        runCatching {
                            listOf(
                                async { pushTokenRegistrar.registerCurrentToken() },
                                async { chatEngine.preloadMe() },
                            ).awaitAll()
                        }
                        eventEmitter.invoke(LoginEvent.LoginComplete)
                    }

                    is LoginResult.Error -> {
                        errorTracker.track(result.cause)
                        dispatch(LoginAction.UpdateContent(LoginScreenState.Content.Error(result.cause)))
                    }

                    LoginResult.MissingWellKnown -> {
                        eventEmitter.invoke(LoginEvent.WellKnownMissing)
                        dispatch(LoginAction.UpdateState(LoginScreenState(showServerUrl = true, content = LoginScreenState.Content.Idle)))
                    }
                }
            }
        }
    },
)

private suspend fun ChatEngine.preloadMe() = this.me(forceRefresh = false)

private fun String?.takeIfNotEmpty() = this?.takeIf { it.isNotEmpty() }
