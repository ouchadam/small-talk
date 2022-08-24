package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.MatrixLogTag
import app.dapk.st.matrix.common.MatrixLogger
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.matrixLog
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
            roomStore.persist(roomId, newState)
        }
    }

    suspend fun remove(roomsLeft: List<RoomId>) {
        roomsLeft.forEach { roomCache.remove(it) }
        roomStore.remove(roomsLeft)
    }
}