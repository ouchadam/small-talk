package app.dapk.st.directory.state

import app.dapk.st.engine.DirectoryState
import app.dapk.state.Action

internal sealed interface ComponentLifecycle : Action {
    object OnVisible : ComponentLifecycle
    object OnGone : ComponentLifecycle
}

internal sealed interface DirectorySideEffect : Action {
    object ScrollToTop : DirectorySideEffect
}

internal sealed interface DirectoryStateChange : Action {
    object Empty : DirectoryStateChange
    data class Content(val content: DirectoryState) : DirectoryStateChange
}
