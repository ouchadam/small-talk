package app.dapk.st.notifications

import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class RenderNotificationsUseCase(
    private val notificationRenderer: NotificationRenderer,
    private val observeRenderableUnreadEventsUseCase: ObserveUnreadNotificationsUseCase,
    private val notificationChannels: NotificationChannels,
) {

    suspend fun listenForNotificationChanges() {
        observeRenderableUnreadEventsUseCase()
            .onStart { notificationChannels.initChannels() }
            .onEach { (each, diff) -> renderUnreadChange(each, diff) }
            .collect()
    }

    private suspend fun renderUnreadChange(allUnread: Map<RoomOverview, List<RoomEvent>>, diff: NotificationDiff) {
        notificationRenderer.render(
            NotificationState(
                allUnread = allUnread,
                removedRooms = diff.removed.keys,
                roomsWithNewEvents = diff.changedOrNew.keys,
                newRooms = diff.newRooms,
            )
        )
    }
}
