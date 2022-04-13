package app.dapk.st.login

sealed interface LoginScreenState {

    data class Content(val showServerUrl: Boolean) : LoginScreenState
    object Loading : LoginScreenState
    data class Error(val cause: Throwable) : LoginScreenState
}

sealed interface LoginEvent {
    object LoginComplete : LoginEvent
    object WellKnownMissing : LoginEvent
}

