package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.internal.request.ApiSyncRoom

internal data class RoomToProcess(
    val roomId: RoomId,
    val apiSyncRoom: ApiSyncRoom,
    val directMessage: UserId?,
    val userCredentials: UserCredentials,
    val heroes: List<UserId>?,
)