package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag.NOTIFICATION
import app.dapk.st.core.log
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class NotificationsUseCase(
    private val notificationRenderer: NotificationRenderer,
    private val observeRenderableUnreadEventsUseCase: ObserveUnreadNotificationsUseCase,
    notificationChannels: NotificationChannels,
) {

    init {
        notificationChannels.initChannels()
    }

    suspend fun listenForNotificationChanges() {
        observeRenderableUnreadEventsUseCase()
            .onEach { (each, diff) -> renderUnreadChange(each, diff) }
            .collect()
    }

    private suspend fun renderUnreadChange(allUnread: Map<RoomOverview, List<RoomEvent>>, diff: NotificationDiff) {
        log(NOTIFICATION, "unread changed - render notifications")
        notificationRenderer.render(
            allUnread = allUnread,
            removedRooms = diff.removed.keys,
            roomsWithNewEvents = diff.changedOrNew.keys,
            newRooms = diff.newRooms,
        )
    }
}
