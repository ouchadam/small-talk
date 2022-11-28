package app.dapk.st.directory.state

import app.dapk.st.state.State
import app.dapk.st.engine.DirectoryState

typealias DirectoryState = State<DirectoryScreenState, DirectoryEvent>

sealed interface DirectoryScreenState {
    object EmptyLoading : DirectoryScreenState
    object Empty : DirectoryScreenState
    data class Content(
        val overviewState: DirectoryState,
    ) : DirectoryScreenState
}

sealed interface DirectoryEvent {
    data class OpenDownloadUrl(val url: String) : DirectoryEvent
    object ScrollToTop : DirectoryEvent
}
