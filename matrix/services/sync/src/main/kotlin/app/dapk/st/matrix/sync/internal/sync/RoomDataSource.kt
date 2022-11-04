package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.RoomStore

class RoomDataSource(
    private val roomStore: RoomStore,
    private val logger: MatrixLogger,
) {

    private val roomCache = mutableMapOf<RoomId, RoomState>()

    fun contains(roomId: RoomId) = roomCache.containsKey(roomId)

    suspend fun read(roomId: RoomId) = when (val cached = roomCache[roomId]) {
        null -> roomStore.retrieve(roomId)?.also { roomCache[roomId] = it }
        else -> cached
    }

    suspend fun persist(roomId: RoomId, previousState: RoomState?, newState: RoomState) {
        if (newState == previousState) {
            logger.matrixLog(MatrixLogTag.SYNC, "no changes, not persisting")
        } else {
            roomCache[roomId] = newState
            roomStore.persist(roomId, newState.events)
        }
    }

    suspend fun remove(roomsLeft: List<RoomId>) {
        roomsLeft.forEach { roomCache.remove(it) }
        roomStore.remove(roomsLeft)
    }

    suspend fun redact(roomId: RoomId, eventId: EventId) {
        val eventToRedactFromCache = roomCache[roomId]?.events?.find { it.eventId == eventId }
        val redactedEvent = when {
            eventToRedactFromCache != null -> {
                eventToRedactFromCache.redact().also { redacted ->
                    val cachedRoomState = roomCache[roomId]
                    requireNotNull(cachedRoomState)
                    roomCache[roomId] = cachedRoomState.replaceEvent(eventToRedactFromCache, redacted)
                }
            }

            else -> roomStore.findEvent(eventId)?.redact()
        }

        redactedEvent?.let { roomStore.persist(roomId, listOf(it)) }
    }
}

private fun RoomEvent.redact() = RoomEvent.Redacted(this.eventId, this.utcTimestamp, this.author)

private fun RoomState.replaceEvent(old: RoomEvent, new: RoomEvent): RoomState {
    val updatedEvents = this.events.toMutableList().apply {
        remove(old)
        add(new)
    }
    return this.copy(events = updatedEvents)
}