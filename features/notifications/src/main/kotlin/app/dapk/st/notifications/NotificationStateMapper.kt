package app.dapk.st.notifications

class NotificationStateMapper(
    private val roomEventsToNotifiableMapper: RoomEventsToNotifiableMapper,
    private val notificationFactory: NotificationFactory,
) {

    suspend fun mapToNotifications(state: NotificationState): Notifications {
        val messageNotifications = createMessageNotifications(state)
        val roomNotifications = messageNotifications.filterIsInstance<NotificationTypes.Room>()
        val summaryNotification = maybeCreateSummary(roomNotifications)
        return Notifications(summaryNotification, messageNotifications)
    }

    private suspend fun createMessageNotifications(state: NotificationState) = state.allUnread.map { (roomOverview, events) ->
        val messageEvents = roomEventsToNotifiableMapper.map(events)
        when (messageEvents.isEmpty()) {
            true -> NotificationTypes.DismissRoom(roomOverview.roomId)
            false -> notificationFactory.createMessageNotification(messageEvents, roomOverview, state.roomsWithNewEvents, state.newRooms)
        }
    }

    private fun maybeCreateSummary(roomNotifications: List<NotificationTypes.Room>) = when {
        roomNotifications.isNotEmpty() -> {
            notificationFactory.createSummary(roomNotifications)
        }
        else -> null
    }
}

data class Notifications(val summaryNotification: AndroidNotification?, val delegates: List<NotificationTypes>)
