package app.dapk.st.matrix.room.internal

import app.dapk.st.matrix.common.RoomId

fun interface RoomInviteRemover {
    suspend fun remove(roomId: RoomId)
}