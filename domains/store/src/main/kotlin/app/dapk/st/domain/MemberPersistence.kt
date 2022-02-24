package app.dapk.st.domain

import app.dapk.db.DapkDb
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.MemberStore
import kotlinx.serialization.json.Json

class MemberPersistence(
    private val database: DapkDb,
    private val coroutineDispatchers: CoroutineDispatchers,
) : MemberStore {

    override suspend fun insert(roomId: RoomId, members: List<RoomMember>) {
        coroutineDispatchers.withIoContext {
            database.roomMemberQueries.transaction {
                members.forEach {
                    database.roomMemberQueries.insert(
                        user_id = it.id.value,
                        room_id = roomId.value,
                        blob = Json.encodeToString(RoomMember.serializer(), it),
                    )
                }
            }
        }
    }

    override suspend fun query(roomId: RoomId, userIds: List<UserId>): List<RoomMember> {
        return coroutineDispatchers.withIoContext {
            database.roomMemberQueries.selectMembersByRoomAndId(roomId.value, userIds.map { it.value })
                .executeAsList()
                .map { Json.decodeFromString(RoomMember.serializer(), it) }
        }
    }
}