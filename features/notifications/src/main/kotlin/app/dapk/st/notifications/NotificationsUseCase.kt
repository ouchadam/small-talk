package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag.NOTIFICATION
import app.dapk.st.core.log
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomStore
import kotlinx.coroutines.flow.*

class NotificationsUseCase(
    private val roomStore: RoomStore,
    private val notificationRenderer: NotificationRenderer,
    notificationChannels: NotificationChannels,
) {

    private val inferredCurrentNotifications = mutableMapOf<RoomId, List<RoomEvent>>()

    init {
        notificationChannels.initChannels()
    }

    suspend fun listenForNotificationChanges() {
        roomStore.observeUnread()
            .skipFirst()
            .onEach { result ->
                log(NOTIFICATION, "unread changed - render notifications")

                val changes = result.mapKeys { it.key.roomId }

                val asRooms = changes.keys
                val removedRooms = inferredCurrentNotifications.keys - asRooms

                val onlyContainsRemovals =
                    inferredCurrentNotifications.filterKeys { !removedRooms.contains(it) } == changes.filterKeys { !removedRooms.contains(it) }
                inferredCurrentNotifications.clear()
                inferredCurrentNotifications.putAll(changes)

                notificationRenderer.render(result, removedRooms, onlyContainsRemovals)
            }
            .collect()
    }

    private fun <T> Flow<T>.skipFirst() = drop(1)
}
