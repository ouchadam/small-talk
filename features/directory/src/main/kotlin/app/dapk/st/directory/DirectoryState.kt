package app.dapk.st.directory

import app.dapk.st.engine.DirectoryState

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

