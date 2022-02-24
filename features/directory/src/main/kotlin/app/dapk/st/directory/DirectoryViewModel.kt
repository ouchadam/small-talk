package app.dapk.st.directory

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.DapkViewModel
import app.dapk.st.directory.DirectoryScreenState.Content
import app.dapk.st.directory.DirectoryScreenState.EmptyLoading
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DirectoryViewModel(
    private val shortcutHandler: ShortcutHandler,
    private val directoryUseCase: DirectoryUseCase,
) : DapkViewModel<DirectoryScreenState, DirectoryEvent>(
    initialState = EmptyLoading
) {

    private var syncJob: Job? = null

    fun start() {
        syncJob = viewModelScope.launch {
            directoryUseCase.startSyncing().collect()
        }

        viewModelScope.launch {
            directoryUseCase.state().onEach {
                shortcutHandler.onDirectoryUpdate(it.map { it.overview })
                if (it.isNotEmpty()) {
                    state = Content(it)
                }
            }.collect()
        }
    }

    fun stop() {
        syncJob?.cancel()
    }
}

