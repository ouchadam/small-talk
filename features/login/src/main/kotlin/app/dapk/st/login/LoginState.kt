package app.dapk.st.login

sealed interface LoginScreenState {

    object Idle : LoginScreenState
    object Loading : LoginScreenState
    data class Error(val cause: Throwable) : LoginScreenState
}

sealed interface LoginEvent {
    object LoginComplete : LoginEvent
}

