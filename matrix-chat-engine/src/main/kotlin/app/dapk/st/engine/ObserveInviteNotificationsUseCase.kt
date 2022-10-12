package app.dapk.st.engine

import app.dapk.st.matrix.sync.InviteMeta
import app.dapk.st.matrix.sync.OverviewStore
import app.dapk.st.matrix.sync.RoomInvite
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

internal typealias ObserveInviteNotificationsUseCase = () -> Flow<InviteNotification>

class ObserveInviteNotificationsUseCaseImpl(private val overviewStore: OverviewStore) : ObserveInviteNotificationsUseCase {

    override fun invoke(): Flow<InviteNotification> {
        return overviewStore.latestInvites()
            .diff()
            .drop(1)
            .flatten()
            .map {
                val text = when (val meta = it.inviteMeta) {
                    InviteMeta.DirectMessage -> "${it.inviterName()} has invited you to chat"
                    is InviteMeta.Room -> "${it.inviterName()} has invited you to ${meta.roomName ?: "unnamed room"}"
                }
                InviteNotification(content = text, roomId = it.roomId)
            }
    }

    private fun Flow<List<RoomInvite>>.diff(): Flow<Set<RoomInvite>> {
        val previousInvites = mutableSetOf<RoomInvite>()
        return this.distinctUntilChanged()
            .map {
                val diff = it.toSet() - previousInvites
                previousInvites.clear()
                previousInvites.addAll(it)
                diff
            }
    }

    private fun RoomInvite.inviterName() = this.from.displayName?.let { "$it (${this.from.id.value})" } ?: this.from.id.value
}

@OptIn(FlowPreview::class)
private fun <T> Flow<Set<T>>.flatten() = this.flatMapConcat { items ->
    flow { items.forEach { this.emit(it) } }
}
