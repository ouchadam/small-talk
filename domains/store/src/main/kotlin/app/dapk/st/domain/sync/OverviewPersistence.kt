package app.dapk.st.domain.sync

import app.dapk.db.DapkDb
import app.dapk.db.model.OverviewStateQueries
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.OverviewState
import app.dapk.st.matrix.sync.OverviewStore
import app.dapk.st.matrix.sync.RoomInvite
import app.dapk.st.matrix.sync.RoomOverview
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val json = Json

internal class OverviewPersistence(
    private val database: DapkDb,
    private val dispatchers: CoroutineDispatchers,
) : OverviewStore {

    override fun latest(): Flow<OverviewState> {
        return database.overviewStateQueries.selectAll()
            .asFlow()
            .mapToList()
            .map { it.map { json.decodeFromString(RoomOverview.serializer(), it.blob) } }
    }

    override suspend fun persistInvites(invites: List<RoomInvite>) {
        dispatchers.withIoContext {
            database.inviteStateQueries.transaction {
                invites.forEach {
                    database.inviteStateQueries.insert(it.roomId.value, json.encodeToString(RoomInvite.serializer(), it))
                }
            }
        }
    }

    override fun latestInvites(): Flow<List<RoomInvite>> {
        return database.inviteStateQueries.selectAll()
            .asFlow()
            .mapToList()
            .map { it.map { json.decodeFromString(RoomInvite.serializer(), it.blob) } }
    }

    override suspend fun persist(overviewState: OverviewState) {
        dispatchers.withIoContext {
            database.transaction {
                overviewState.forEach {
                    database.overviewStateQueries.insertStateOverview(it)
                }
            }
        }
    }

    override suspend fun retrieve(): OverviewState {
        return dispatchers.withIoContext {
            val overviews = database.overviewStateQueries.selectAll().executeAsList()
            overviews.map { json.decodeFromString(RoomOverview.serializer(), it.blob) }
        }
    }

    internal fun retrieve(roomId: RoomId): RoomOverview? {
        return database.overviewStateQueries.selectRoom(roomId.value).executeAsOneOrNull()?.let {
            json.decodeFromString(RoomOverview.serializer(), it)
        }
    }
}

private fun OverviewStateQueries.insertStateOverview(roomOverview: RoomOverview) {
    this.insert(
        room_id = roomOverview.roomId.value,
        latest_activity_timestamp_utc = roomOverview.lastMessage?.utcTimestamp ?: roomOverview.roomCreationUtc,
        blob = json.encodeToString(RoomOverview.serializer(), roomOverview),
        read_marker = roomOverview.readMarker?.value
    )
}
