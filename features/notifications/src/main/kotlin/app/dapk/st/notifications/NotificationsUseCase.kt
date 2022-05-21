package app.dapk.st.notifications

import app.dapk.st.core.AppLogTag.NOTIFICATION
import app.dapk.st.core.log
import app.dapk.st.matrix.common.EventId
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
    private var previousUnreadEvents: Map<RoomId, List<EventId>>? = null

    init {
        notificationChannels.initChannels()
    }

    data class NotificationDiff(
        val unchanged: Map<RoomId, List<EventId>>,
        val changedOrNew: Map<RoomId, List<EventId>>,
        val removed: Map<RoomId, List<EventId>>
    )

    suspend fun listenForNotificationChanges() {
        roomStore.observeUnread()
            .map { each ->
                val allUnreadIds = each.toIds()
                val notificationDiff = calculateDiff(allUnreadIds, previousUnreadEvents)
                previousUnreadEvents = allUnreadIds
                each to notificationDiff
            }
            .skipFirst()
            .onEach { (each, diff) ->
                when {
                    diff.changedOrNew.isEmpty() && diff.removed.isEmpty() -> {
                        log(NOTIFICATION, "Ignoring unread change due to no renderable changes")
                    }
                    inferredCurrentNotifications.isEmpty() && diff.removed.isNotEmpty() -> {
                        log(NOTIFICATION, "Ignoring unread change due to no currently showing messages and changes are all messages marked as read")
                    }
                    else -> renderUnreadChange(each, diff)
                }
            }
            .collect()
    }

    private fun calculateDiff(allUnread: Map<RoomId, List<EventId>>, previousUnread: Map<RoomId, List<EventId>>?): NotificationDiff {
        val unchanged = previousUnread?.filter { allUnread.containsKey(it.key) && it.value == allUnread[it.key] } ?: emptyMap()
        val changedOrNew = allUnread.filterNot { unchanged.containsKey(it.key) }
        val removed = previousUnread?.filter { !unchanged.containsKey(it.key) } ?: emptyMap()
        return NotificationDiff(unchanged, changedOrNew, removed)
    }

    private suspend fun renderUnreadChange(allUnread: Map<RoomOverview, List<RoomEvent>>, diff: NotificationDiff) {
        log(NOTIFICATION, "unread changed - render notifications")
        inferredCurrentNotifications.clear()
        inferredCurrentNotifications.putAll(allUnread.mapKeys { it.key.roomId })

        notificationRenderer.render(
            allUnread = allUnread,
            removedRooms = diff.removed.keys,
            roomsWithNewEvents = diff.changedOrNew.keys
        )
    }

    private fun <T> Flow<T>.skipFirst() = drop(1)
}

private fun List<RoomEvent>.toEventIds() = this.map { it.eventId }
private fun Map<RoomOverview, List<RoomEvent>>.toIds() = this
    .mapValues { it.value.toEventIds() }
    .mapKeys { it.key.roomId }
