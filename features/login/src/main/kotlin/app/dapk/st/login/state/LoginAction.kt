package app.dapk.st.login.state

import app.dapk.state.Action

sealed interface LoginAction : Action {

    sealed interface ComponentLifecycle : LoginAction {
        object Visible : ComponentLifecycle
    }

    data class Login(val userName: String, val password: String, val serverUrl: String?) : LoginAction

    data class UpdateContent(val content: LoginScreenState.Content) : LoginAction
    data class UpdateState(val state: LoginScreenState) : LoginAction
}