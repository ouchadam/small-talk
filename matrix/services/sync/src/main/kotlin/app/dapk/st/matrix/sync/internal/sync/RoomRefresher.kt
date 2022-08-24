package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.internal.room.RoomEventsDecrypter

internal class RoomRefresher(
    private val roomDataSource: RoomDataSource,
    private val roomEventsDecrypter: RoomEventsDecrypter,
    private val logger: MatrixLogger
) {

    suspend fun refreshRoomContent(roomId: RoomId, userCredentials: UserCredentials): RoomState? {
        logger.matrixLog(MatrixLogTag.SYNC, "reducing side effect: $roomId")
        return when (val previousState = roomDataSource.read(roomId)) {
            null -> null.also { logger.matrixLog(MatrixLogTag.SYNC, "no previous state to update") }
            else -> {
                logger.matrixLog(MatrixLogTag.SYNC, "previous state updated")
                val decryptedEvents = previousState.events.decryptEvents(userCredentials)
                val lastMessage = decryptedEvents.sortedByDescending { it.utcTimestamp }.findLastMessage()

                previousState.copy(events = decryptedEvents, roomOverview = previousState.roomOverview.copy(lastMessage = lastMessage)).also {
                    roomDataSource.persist(roomId, previousState, it)
                }
            }
        }
    }

    private suspend fun List<RoomEvent>.decryptEvents(userCredentials: UserCredentials) = roomEventsDecrypter.decryptRoomEvents(userCredentials, this)

}