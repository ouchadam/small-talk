package app.dapk.st.domain.room

import app.dapk.db.DapkDb
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.MuteableStore
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class MutedStorePersistence(
    private val database: DapkDb,
    private val coroutineDispatchers: CoroutineDispatchers,
) : MuteableStore {

    private val allMutedFlow = MutableSharedFlow<Set<RoomId>>(replay = 1)

    override suspend fun mute(roomId: RoomId) {
        coroutineDispatchers.withIoContext {
            database.mutedRoomQueries.insertMuted(roomId.value)
        }
    }

    override suspend fun unmute(roomId: RoomId) {
        coroutineDispatchers.withIoContext {
            database.mutedRoomQueries.removeMuted(roomId.value)
        }
    }

    override suspend fun isMuted(roomId: RoomId) = allMutedFlow.firstOrNull()?.contains(roomId) ?: false

    override fun observeMuted(): Flow<Set<RoomId>> = database.mutedRoomQueries.select()
        .asFlow()
        .mapToList()
        .map { it.map { RoomId(it) }.toSet() }

}