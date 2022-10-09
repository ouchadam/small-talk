package app.dapk.st.share

import app.dapk.st.engine.ChatEngine
import kotlinx.coroutines.flow.first

class FetchRoomsUseCase(
    private val chatEngine: ChatEngine,
) {

    suspend fun fetch(): List<Item> {
        return chatEngine.directory().first().map {
            val overview = it.overview
            Item(
                overview.roomId,
                overview.roomAvatarUrl,
                overview.roomName ?: "",
                chatEngine.findMembersSummary(overview.roomId).map { it.displayName ?: it.id.value }
            )
        }
    }
}