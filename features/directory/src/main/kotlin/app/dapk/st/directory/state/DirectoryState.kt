package app.dapk.st.directory.state

import app.dapk.st.core.State
import app.dapk.st.engine.DirectoryState

internal typealias DirectoryState = State<DirectoryScreenState, DirectoryEvent>

internal sealed interface DirectoryScreenState {
    object EmptyLoading : DirectoryScreenState
    object Empty : DirectoryScreenState
    data class Content(
        val overviewState: DirectoryState,
    ) : DirectoryScreenState
}

internal sealed interface DirectoryEvent {
    data class OpenDownloadUrl(val url: String) : DirectoryEvent
    object ScrollToTop : DirectoryEvent
}
