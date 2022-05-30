package app.dapk.st.share

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.AndroidUri
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.viewmodel.DapkViewModel
import app.dapk.st.viewmodel.MutableStateFactory
import app.dapk.st.viewmodel.defaultStateFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShareEntryViewModel(
    private val fetchRoomsUseCase: FetchRoomsUseCase,
    factory: MutableStateFactory<DirectoryScreenState> = defaultStateFactory(),
) : DapkViewModel<DirectoryScreenState, DirectoryEvent>(
    initialState = DirectoryScreenState.EmptyLoading,
    factory,
) {

    private var syncJob: Job? = null

    fun start() {
        syncJob = viewModelScope.launch {
            state = DirectoryScreenState.Content(fetchRoomsUseCase.bar())
        }
    }

    fun stop() {
        syncJob?.cancel()
    }


    fun sendAttachment() {

    }

    fun withUris(urisToShare: List<Uri>) {
//        TODO("Not yet implemented")
    }

}

class FetchRoomsUseCase(
    private val syncSyncService: SyncService,
    private val roomService: RoomService,
) {

    suspend fun bar(): List<Item> {
        return syncSyncService.overview().first().map {
            Item(
                it.roomId,
                it.roomAvatarUrl,
                it.roomName ?: "",
                roomService.findMembersSummary(it.roomId).map { it.displayName ?: it.id.value }
            )
        }
    }
}

