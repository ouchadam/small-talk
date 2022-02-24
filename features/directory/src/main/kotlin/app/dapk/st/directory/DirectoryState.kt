package app.dapk.st.directory

sealed interface DirectoryScreenState {

    object EmptyLoading : DirectoryScreenState
    data class Error(val cause: Throwable) : DirectoryScreenState
    data class Content(
        val overviewState: DirectoryState,
//        val appState: AppState,
//        val navigationState: NavigationState,
//        val isRefreshing: Boolean = false,
    ) : DirectoryScreenState
}

sealed interface NavigationState {}

sealed interface DirectoryEvent { data class OpenDownloadUrl(val url: String) : DirectoryEvent
}

