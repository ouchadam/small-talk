package app.dapk.st.domain.sync

import app.dapk.db.DapkDb
import app.dapk.db.model.RoomEventQueries
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.matrix.sync.RoomStore
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val json = Json

internal class RoomPersistence(
    private val database: DapkDb,
    private val overviewPersistence: OverviewPersistence,
    private val coroutineDispatchers: CoroutineDispatchers,
) : RoomStore {

    override suspend fun persist(roomId: RoomId, state: RoomState) {
        coroutineDispatchers.withIoContext {
            database.transaction {
                state.events.forEach {
                    database.roomEventQueries.insertRoomEvent(roomId, it)
                }
            }
        }
    }

    override fun latest(roomId: RoomId): Flow<RoomState> {
        val overviewFlow = database.overviewStateQueries.selectRoom(roomId.value).asFlow().mapToOneNotNull().map {
            json.decodeFromString(RoomOverview.serializer(), it)
        }

        return database.roomEventQueries.selectRoom(roomId.value)
            .asFlow()
            .mapToList()
            .map { it.map { json.decodeFromString(RoomEvent.serializer(), it) } }
            .combine(overviewFlow) { events, overview ->
                RoomState(overview, events)
            }
    }

    override suspend fun retrieve(roomId: RoomId): RoomState? {
        return coroutineDispatchers.withIoContext {
            overviewPersistence.retrieve(roomId)?.let { overview ->
                val roomEvents = database.roomEventQueries.selectRoom(roomId.value).executeAsList().map {
                    json.decodeFromString(RoomEvent.serializer(), it)
                }
                RoomState(overview, roomEvents)
            }
        }
    }

    override suspend fun insertUnread(roomId: RoomId, eventIds: List<EventId>) {
        coroutineDispatchers.withIoContext {
            database.transaction {
                eventIds.forEach { eventId ->
                    database.unreadEventQueries.insertUnread(
                        event_id = eventId.value,
                        room_id = roomId.value,
                    )
                }
            }
        }
    }

    override fun observeUnread(): Flow<Map<RoomOverview, List<RoomEvent>>> {
        return database.roomEventQueries.selectAllUnread()
            .asFlow()
            .mapToList()
            .distinctUntilChanged()
            .map {
                it.groupBy { RoomId(it.room_id) }
                    .mapKeys { overviewPersistence.retrieve(it.key)!! }
                    .mapValues {
                        it.value.map {
                            json.decodeFromString(RoomEvent.serializer(), it.blob)
                        }
                    }
            }
    }

    override fun observeUnreadCountById(): Flow<Map<RoomId, Int>> {
        return database.roomEventQueries.selectAllUnread()
            .asFlow()
            .mapToList()
            .map {
                it.groupBy { RoomId(it.room_id) }
                    .mapValues { it.value.size }
            }
    }

    override suspend fun markRead(roomId: RoomId) {
        coroutineDispatchers.withIoContext {
            database.unreadEventQueries.removeRead(room_id = roomId.value)
        }
    }

    override fun observeEvent(eventId: EventId): Flow<EventId> {
        return database.roomEventQueries.selectEvent(event_id = eventId.value)
            .asFlow()
            .mapToOneNotNull()
            .map { EventId(it) }
    }

    override suspend fun findEvent(eventId: EventId): RoomEvent? {
        return coroutineDispatchers.withIoContext {
            database.roomEventQueries.selectEventContent(event_id = eventId.value)
                .executeAsOneOrNull()
                ?.let { json.decodeFromString(RoomEvent.serializer(), it) }
        }
    }
}

private fun RoomEventQueries.insertRoomEvent(roomId: RoomId, roomEvent: RoomEvent) {
    this.insert(
        app.dapk.db.model.DbRoomEvent(
            event_id = roomEvent.eventId.value,
            room_id = roomId.value,
            timestamp_utc = roomEvent.utcTimestamp,
            blob = json.encodeToString(RoomEvent.serializer(), roomEvent),
        )
    )
}
