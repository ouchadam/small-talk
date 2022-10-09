package app.dapk.st.share

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.AndroidUri
import app.dapk.st.viewmodel.DapkViewModel
import app.dapk.st.viewmodel.MutableStateFactory
import app.dapk.st.viewmodel.defaultStateFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ShareEntryViewModel(
    private val fetchRoomsUseCase: FetchRoomsUseCase,
    factory: MutableStateFactory<DirectoryScreenState> = defaultStateFactory(),
) : DapkViewModel<DirectoryScreenState, DirectoryEvent>(
    initialState = DirectoryScreenState.EmptyLoading,
    factory,
) {

    private var urisToShare: List<AndroidUri>? = null
    private var syncJob: Job? = null

    fun start() {
        syncJob = viewModelScope.launch {
            state = DirectoryScreenState.Content(fetchRoomsUseCase.fetch())
        }
    }

    fun stop() {
        syncJob?.cancel()
    }

    fun withUris(urisToShare: List<Uri>) {
        this.urisToShare = urisToShare.map { AndroidUri(it.toString()) }
    }

    fun onRoomSelected(item: Item) {
        viewModelScope.launch {
            _events.emit(DirectoryEvent.SelectRoom(item, uris = urisToShare ?: throw IllegalArgumentException("Not uris set")))
        }
    }

}
