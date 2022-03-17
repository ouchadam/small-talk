package app.dapk.st.matrix.room

import app.dapk.st.matrix.MatrixService
import app.dapk.st.matrix.MatrixServiceInstaller
import app.dapk.st.matrix.MatrixServiceProvider
import app.dapk.st.matrix.ServiceDepFactory
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.internal.DefaultRoomService
import app.dapk.st.matrix.room.internal.RoomMembers

private val SERVICE_KEY = RoomService::class

interface RoomService : MatrixService {

    suspend fun joinedMembers(roomId: RoomId): List<JoinedMember>
    suspend fun markFullyRead(roomId: RoomId, eventId: EventId)

    suspend fun findMember(roomId: RoomId, userId: UserId): RoomMember?
    suspend fun findMembers(roomId: RoomId, userIds: List<UserId>): List<RoomMember>
    suspend fun insertMembers(roomId: RoomId, members: List<RoomMember>)

    suspend fun createDm(userId: UserId, encrypted: Boolean): RoomId

    suspend fun joinRoom(roomId: RoomId)
    suspend fun rejectJoinRoom(roomId: RoomId)

    data class JoinedMember(
        val userId: UserId,
        val displayName: String,
        val avatarUrl: String?,
    )

}

fun MatrixServiceInstaller.installRoomService(
    memberStore: MemberStore,
    roomMessenger: ServiceDepFactory<RoomMessenger>,
) {
    this.install { (httpClient, _, services, logger) ->
        SERVICE_KEY to DefaultRoomService(httpClient, logger, RoomMembers(memberStore), roomMessenger.create(services))
    }
}

fun MatrixServiceProvider.roomService(): RoomService = this.getService(key = SERVICE_KEY)

interface MemberStore {
    suspend fun insert(roomId: RoomId, members: List<RoomMember>)
    suspend fun query(roomId: RoomId, userIds: List<UserId>): List<RoomMember>
}

interface RoomMessenger {
    suspend fun enableEncryption(roomId: RoomId)
}