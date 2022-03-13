package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag.NOTIFICATION
import app.dapk.st.core.log
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach

class NotificationsUseCase(
    private val roomStore: RoomStore,
    private val notificationRenderer: NotificationRenderer,
    notificationChannels: NotificationChannels,
) {

    private val inferredCurrentNotifications = mutableSetOf<RoomId>()

    init {
        notificationChannels.initChannels()
    }

    suspend fun listenForNotificationChanges() {
        roomStore.observeUnread()
            .drop(1)
            .onEach { result ->
                log(NOTIFICATION, "unread changed - render notifications")

                val asRooms = result.keys.map { it.roomId }.toSet()
                val removedRooms = inferredCurrentNotifications - asRooms

                inferredCurrentNotifications.clear()
                inferredCurrentNotifications.addAll(asRooms)

                notificationRenderer.render(result, removedRooms)
            }
            .collect()
    }
}
