package app.dapk.st.directory

sealed interface DirectoryScreenState {

    object EmptyLoading : DirectoryScreenState
    data class Content(
        val overviewState: DirectoryState,
    ) : DirectoryScreenState
}

sealed interface DirectoryEvent {
    data class OpenDownloadUrl(val url: String) : DirectoryEvent
}

