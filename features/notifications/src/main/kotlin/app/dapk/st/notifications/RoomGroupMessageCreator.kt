package app.dapk.st.notifications

import app.dapk.st.imageloader.IconLoader

class RoomGroupMessageCreator(
    private val iconLoader: IconLoader,
//    private val bitmapLoader: BitmapLoader,
//    private val stringProvider: StringProvider,
//    private val notificationUtils: NotificationUtils,
//    private val appContext: Context
) {

//    fun createRoomMessage(events: List<NotifiableMessageEvent>, roomId: String, userDisplayName: String, userAvatarUrl: String?): RoomNotification.Message {
//        val firstKnownRoomEvent = events[0]
//        val roomName = firstKnownRoomEvent.roomName ?: firstKnownRoomEvent.senderName ?: ""
//        val roomIsGroup = !firstKnownRoomEvent.roomIsDirect
//
//        val style = Notification.MessagingStyle(
//            Person.Builder()
//                .setName(userDisplayName)
//                .setIcon(iconLoader.load(userAvatarUrl))
//                .setKey(firstKnownRoomEvent.matrixID)
//                .build()
//        ).also {
//            it.conversationTitle = roomName.takeIf { roomIsGroup }
//            it.isGroupConversation = roomIsGroup
//            it.addMessagesFromEvents(events)
//        }
//
//        val tickerText = if (roomIsGroup) {
//            stringProvider.getString(R.string.notification_ticker_text_group, roomName, events.last().senderName, events.last().description)
//        } else {
//            stringProvider.getString(R.string.notification_ticker_text_dm, events.last().senderName, events.last().description)
//        }
//
//        val largeBitmap = getRoomBitmap(events)
//
//        val lastMessageTimestamp = events.last().timestamp
//        val smartReplyErrors = events.filter { it.isSmartReplyError() }
//        val messageCount = (events.size - smartReplyErrors.size)
//        val meta = RoomNotification.Message.Meta(
//            summaryLine = createRoomMessagesGroupSummaryLine(events, roomName, roomIsDirect = !roomIsGroup),
//            messageCount = messageCount,
//            latestTimestamp = lastMessageTimestamp,
//            roomId = roomId,
//            shouldBing = events.any { it.noisy }
//        )
//        return RoomNotification.Message(
//            notificationUtils.buildMessagesListNotification(
//                style,
//                RoomEventGroupInfo(roomId, roomName, isDirect = !roomIsGroup),
//                largeIcon = largeBitmap,
//                lastMessageTimestamp,
//                userDisplayName,
//                tickerText
//            ),
//            meta
//        )
//    }

//    private fun Notification.MessagingStyle.addMessagesFromEvents(events: List<NotifiableMessageEvent>) {
//        events.forEach { event ->
//            val senderPerson = if (event.outGoingMessage) {
//                null
//            } else {
//                Person.Builder()
//                    .setName(event.senderName)
//                    .setIcon(iconLoader.getUserIcon(event.senderAvatarPath))
//                    .setKey(event.senderId)
//                    .build()
//            }
//            when {
//                event.isSmartReplyError() -> addMessage(stringProvider.getString(R.string.notification_inline_reply_failed), event.timestamp, senderPerson)
//                else -> {
//                    val message = Notification.MessagingStyle.Message(event.body, event.timestamp, senderPerson).also { message ->
//                        event.imageUri?.let {
//                            message.setData("image/", it)
//                        }
//                    }
//                    addMessage(message)
//                }
//            }
//        }
//    }
//
//    private fun createRoomMessagesGroupSummaryLine(events: List<NotifiableMessageEvent>, roomName: String, roomIsDirect: Boolean): CharSequence {
//        return when (events.size) {
//            1 -> createFirstMessageSummaryLine(events.first(), roomName, roomIsDirect)
//            else -> {
//                stringProvider.getQuantityString(
//                    R.plurals.notification_compat_summary_line_for_room,
//                    events.size,
//                    roomName,
//                    events.size
//                )
//            }
//        }
//    }
//
//    private fun createFirstMessageSummaryLine(event: NotifiableMessageEvent, roomName: String, roomIsDirect: Boolean): Span {
//        return if (roomIsDirect) {
//            buildSpannedString {
//                bold { append("${event.senderName}: ") }
//                append(event.description)
//            }
//        } else {
//            buildSpannedString {
//                bold { append("$roomName: ${event.senderName} ") }
//                append(event.description)
//            }
//        }
//    }
//
//    private fun getRoomBitmap(events: List<NotifiableMessageEvent>): Bitmap? {
//        // Use the last event (most recent?)
//        return events.lastOrNull()
//            ?.roomAvatarPath
//            ?.let { bitmapLoader.getRoomBitmap(it) }
//    }
}

//data class RoomMessage(
//
//)