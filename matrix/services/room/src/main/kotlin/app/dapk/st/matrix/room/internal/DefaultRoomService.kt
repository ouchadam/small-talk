package app.dapk.st.matrix.room.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.MatrixLogTag.ROOM
import app.dapk.st.matrix.http.MatrixHttpClient
import app.dapk.st.matrix.http.MatrixHttpClient.HttpRequest.Companion.httpRequest
import app.dapk.st.matrix.http.emptyJsonBody
import app.dapk.st.matrix.http.jsonBody
import app.dapk.st.matrix.room.RoomMessenger
import app.dapk.st.matrix.room.RoomService
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DefaultRoomService(
    private val httpClient: MatrixHttpClient,
    private val logger: MatrixLogger,
    private val roomMembers: RoomMembers,
    private val roomMessenger: RoomMessenger,
    private val roomInviteRemover: RoomInviteRemover,
    private val singleRoomStore: SingleRoomStore,
) : RoomService {

    override suspend fun joinedMembers(roomId: RoomId): List<RoomService.JoinedMember> {
        val response = httpClient.execute(joinedMembersRequest(roomId))
        return response.joined.map { (userId, member) ->
            RoomService.JoinedMember(userId, member.displayName, member.avatarUrl)
        }.also {
            logger.matrixLog(ROOM, "found members for $roomId : size: ${it.size}")
        }
    }

    override suspend fun markFullyRead(roomId: RoomId, eventId: EventId, isPrivate: Boolean) {
        logger.matrixLog(ROOM, "marking room fully read ${roomId.value}")
        httpClient.execute(markFullyReadRequest(roomId, eventId, isPrivate))
    }

    override suspend fun findMember(roomId: RoomId, userId: UserId): RoomMember? {
        return roomMembers.findMember(roomId, userId)
    }

    override suspend fun findMembers(roomId: RoomId, userIds: List<UserId>): List<RoomMember> {
        return roomMembers.findMembers(roomId, userIds)
    }

    override suspend fun findMembersSummary(roomId: RoomId): List<RoomMember> {
        return roomMembers.findMembersSummary(roomId)
    }

    override suspend fun insertMembers(roomId: RoomId, members: List<RoomMember>) {
        roomMembers.insert(roomId, members)
    }

    override suspend fun createDm(userId: UserId, encrypted: Boolean): RoomId {
        logger.matrixLog("creating DM $userId")
        val roomResponse = httpClient.execute(
            createRoomRequest(
                invites = listOf(userId),
                isDM = true,
                visibility = RoomVisibility.private
            )
        )

        if (encrypted) {
            roomMessenger.enableEncryption(roomResponse.roomId)
        }
        return roomResponse.roomId
    }

    override suspend fun joinRoom(roomId: RoomId) {
        httpClient.execute(joinRoomRequest(roomId))
    }

    override suspend fun rejectJoinRoom(roomId: RoomId) {
        runCatching { httpClient.execute(rejectJoinRoomRequest(roomId)) }.fold(
            onSuccess = {},
            onFailure = {
                when (it) {
                    is ClientRequestException -> {
                        if (it.response.status == HttpStatusCode.Forbidden) {
                            // allow error
                        } else {
                            throw it
                        }

                    }

                    else -> throw it
                }
            }
        )
        roomInviteRemover.remove(roomId)
    }

    override suspend fun muteRoom(roomId: RoomId) {
        singleRoomStore.mute(roomId)
    }

    override suspend fun unmuteRoom(roomId: RoomId) {
        singleRoomStore.unmute(roomId)
    }

    override fun observeIsMuted(roomId: RoomId): Flow<Boolean> = singleRoomStore.isMuted(roomId)
}

interface SingleRoomStore {
    suspend fun mute(roomId: RoomId)
    suspend fun unmute(roomId: RoomId)
    fun isMuted(roomId: RoomId): Flow<Boolean>
}

internal fun joinedMembersRequest(roomId: RoomId) = httpRequest<JoinedMembersResponse>(
    path = "_matrix/client/r0/rooms/${roomId.value}/joined_members",
    method = MatrixHttpClient.Method.GET,
)

internal fun markFullyReadRequest(roomId: RoomId, eventId: EventId, isPrivate: Boolean) = httpRequest<Unit>(
    path = "_matrix/client/r0/rooms/${roomId.value}/read_markers",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(MarkFullyReadRequest(eventId, eventId, hidden = isPrivate))
)

internal fun createRoomRequest(invites: List<UserId>, isDM: Boolean, visibility: RoomVisibility, name: String? = null) = httpRequest<ApiCreateRoomResponse>(
    path = "_matrix/client/r0/createRoom",
    method = MatrixHttpClient.Method.POST,
    body = jsonBody(CreateRoomRequest(invites, isDM, visibility, name))
)

internal fun joinRoomRequest(roomId: RoomId) = httpRequest<Unit>(
    path = "_matrix/client/r0/rooms/${roomId.value}/join",
    method = MatrixHttpClient.Method.POST,
    body = emptyJsonBody()
)

internal fun rejectJoinRoomRequest(roomId: RoomId) = httpRequest<Unit>(
    path = "_matrix/client/r0/rooms/${roomId.value}/leave",
    method = MatrixHttpClient.Method.POST,
    body = emptyJsonBody()
)


@Suppress("EnumEntryName")
@Serializable
enum class RoomVisibility {
    public, private
}

@Serializable
internal data class CreateRoomRequest(
    @SerialName("invite") val invites: List<UserId>,
    @SerialName("is_direct") val isDM: Boolean,
    @SerialName("visibility") val visibility: RoomVisibility,
    @SerialName("name") val name: String? = null,
)

@Serializable
internal data class ApiCreateRoomResponse(
    @SerialName("room_id") val roomId: RoomId,
)

@Serializable
internal data class MarkFullyReadRequest(
    @SerialName("m.fully_read") val eventId: EventId,
    @SerialName("m.read") val read: EventId,
    @SerialName("m.hidden") val hidden: Boolean
)

@Serializable
internal data class JoinedMembersResponse(
    @SerialName("joined") val joined: Map<UserId, ApiJoinedMember>
)

@Serializable
internal data class ApiJoinedMember(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)