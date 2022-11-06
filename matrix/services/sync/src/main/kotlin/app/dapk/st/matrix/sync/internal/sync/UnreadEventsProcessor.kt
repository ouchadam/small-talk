package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.MatrixLogTag
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.common.matrixLog
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomStore

internal class UnreadEventsProcessor(
    private val roomStore: RoomStore,
    private val logger: MatrixLogger,
) {

    suspend fun processUnreadState(
        overview: RoomOverview,
        previousState: RoomOverview?,
        newEvents: List<RoomEvent>,
        selfId: UserId,
        isInitialSync: Boolean,
    ) {
        val areWeViewingRoom = false // TODO

        when {
            isInitialSync -> {
                // let's assume everything is read
            }

            previousState?.readMarker != overview.readMarker -> {
                // assume the user has viewed the room
                logger.matrixLog(MatrixLogTag.SYNC, "marking room read due to new read marker")
                roomStore.markRead(overview.roomId)
            }

            areWeViewingRoom -> {
                logger.matrixLog(MatrixLogTag.SYNC, "marking room read")
                roomStore.markRead(overview.roomId)
            }

            newEvents.isNotEmpty() -> {
                logger.matrixLog(MatrixLogTag.SYNC, "insert new unread events")

                val eventsFromOthers = newEvents.filterNot {
                    when (it) {
                        is RoomEvent.Message -> it.author.id == selfId
                        is RoomEvent.Reply -> it.message.author.id == selfId
                        is RoomEvent.Image -> it.author.id == selfId
                        is RoomEvent.Encrypted -> it.author.id == selfId
                        is RoomEvent.Redacted -> it.author.id == selfId
                    }
                }.map { it.eventId }
                roomStore.insertUnread(overview.roomId, eventsFromOthers)
            }
        }
    }

}