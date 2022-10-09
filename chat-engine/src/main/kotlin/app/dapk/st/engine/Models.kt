package app.dapk.st.engine

import app.dapk.st.matrix.common.*

typealias DirectoryState = List<DirectoryItem>
typealias OverviewState = List<RoomOverview>
typealias InviteState = List<RoomInvite>

data class DirectoryItem(
    val overview: RoomOverview,
    val unreadCount: UnreadCount,
    val typing: Typing?
)

data class RoomOverview(
    val roomId: RoomId,
    val roomCreationUtc: Long,
    val roomName: String?,
    val roomAvatarUrl: AvatarUrl?,
    val lastMessage: LastMessage?,
    val isGroup: Boolean,
    val readMarker: EventId?,
    val isEncrypted: Boolean,
) {

    data class LastMessage(
        val content: String,
        val utcTimestamp: Long,
        val author: RoomMember,
    )

}

data class RoomInvite(
    val from: RoomMember,
    val roomId: RoomId,
    val inviteMeta: InviteMeta,
) {
    sealed class InviteMeta {
        object DirectMessage : InviteMeta()
        data class Room(val roomName: String? = null) : InviteMeta()
    }

}

@JvmInline
value class UnreadCount(val value: Int)

data class Typing(val roomId: RoomId, val members: List<RoomMember>)

data class LoginRequest(val userName: String, val password: String, val serverUrl: String?)

sealed interface LoginResult {
    data class Success(val userCredentials: UserCredentials) : LoginResult
    object MissingWellKnown : LoginResult
    data class Error(val cause: Throwable) : LoginResult
}

data class Me(
    val userId: UserId,
    val displayName: String?,
    val avatarUrl: AvatarUrl?,
    val homeServerUrl: HomeServerUrl,
)

sealed interface ImportResult {
    data class Success(val roomIds: Set<RoomId>, val totalImportedKeysCount: Long) : ImportResult
    data class Error(val cause: Type) : ImportResult {

        sealed interface Type {
            data class Unknown(val cause: Throwable) : Type
            object NoKeysFound : Type
            object UnexpectedDecryptionOutput : Type
            object UnableToOpenFile : Type
            object InvalidFile : Type
        }

    }

    data class Update(val importedKeysCount: Long) : ImportResult
}
