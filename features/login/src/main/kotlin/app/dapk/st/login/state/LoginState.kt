package app.dapk.st.login.state

import app.dapk.st.state.State

typealias LoginState = State<LoginScreenState, LoginEvent>

data class LoginScreenState(
    val showServerUrl: Boolean,
    val content: Content,
) {

    sealed interface Content {
        object Idle : Content
        object Loading : Content
        data class Error(val cause: Throwable) : Content
    }
}

sealed interface LoginEvent {
    object LoginComplete : LoginEvent
    object WellKnownMissing : LoginEvent
}
