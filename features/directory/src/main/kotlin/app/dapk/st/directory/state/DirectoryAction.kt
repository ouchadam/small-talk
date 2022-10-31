package app.dapk.st.directory.state

import app.dapk.st.engine.DirectoryState
import app.dapk.state.Action

sealed interface ComponentLifecycle : Action {
    object OnVisible : ComponentLifecycle
    object OnGone : ComponentLifecycle
}

sealed interface DirectorySideEffect : Action {
    object ScrollToTop : DirectorySideEffect
}

sealed interface DirectoryStateChange : Action {
    object Empty : DirectoryStateChange
    data class Content(val content: DirectoryState) : DirectoryStateChange
}
