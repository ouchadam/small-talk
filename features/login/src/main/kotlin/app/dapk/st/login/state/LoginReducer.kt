package app.dapk.st.login.state

import app.dapk.st.core.logP
import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.state.async
import app.dapk.state.change
import app.dapk.state.createReducer
import kotlinx.coroutines.launch

fun loginReducer(
    loginUseCase: LoginUseCase,
    eventEmitter: suspend (LoginEvent) -> Unit,
) = createReducer(
    initialState = LoginScreenState(showServerUrl = false, content = LoginScreenState.Content.Idle),

    change(LoginAction.ComponentLifecycle.Visible::class) { _, state ->
        LoginScreenState(state.showServerUrl, content = LoginScreenState.Content.Idle)
    },

    change(LoginAction.UpdateContent::class) { action, state -> state.copy(content = action.content) },

    change(LoginAction.UpdateState::class) { action, _ -> action.state },

    async(LoginAction.Login::class) { action ->
        coroutineScope.launch {
            logP("login") {
                dispatch(LoginAction.UpdateContent(LoginScreenState.Content.Loading))
                val request = LoginRequest(action.userName, action.password, action.serverUrl.takeIfNotEmpty())

                when (val result = loginUseCase.login(request)) {
                    is LoginResult.Error -> dispatch(LoginAction.UpdateContent(LoginScreenState.Content.Error(result.cause)))

                    LoginResult.MissingWellKnown -> {
                        eventEmitter.invoke(LoginEvent.WellKnownMissing)
                        dispatch(LoginAction.UpdateState(LoginScreenState(showServerUrl = true, content = LoginScreenState.Content.Idle)))
                    }

                    is LoginResult.Success -> eventEmitter.invoke(LoginEvent.LoginComplete)
                }
            }
        }
    },
)

private fun String?.takeIfNotEmpty() = this?.takeIf { it.isNotEmpty() }
