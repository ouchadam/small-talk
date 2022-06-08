package app.dapk.st.share

import app.dapk.st.core.AndroidUri
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.RoomId

sealed interface DirectoryScreenState {

    object EmptyLoading : DirectoryScreenState
    object Empty : DirectoryScreenState
    data class Content(
        val items: List<Item>,
    ) : DirectoryScreenState
}

sealed interface DirectoryEvent {
    data class SelectRoom(val item: Item, val uris: List<AndroidUri>) : DirectoryEvent
}

data class Item(val id: RoomId, val roomAvatarUrl: AvatarUrl?, val roomName: String, val members: List<String>)
