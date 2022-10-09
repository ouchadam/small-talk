package app.dapk.st.engine

import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.sync.InviteMeta
import app.dapk.st.matrix.auth.AuthService.LoginRequest as MatrixLoginRequest
import app.dapk.st.matrix.auth.AuthService.LoginResult as MatrixLoginResult
import app.dapk.st.matrix.crypto.ImportResult as MatrixImportResult
import app.dapk.st.matrix.room.ProfileService.Me as MatrixMe
import app.dapk.st.matrix.sync.LastMessage as MatrixLastMessage
import app.dapk.st.matrix.sync.RoomInvite as MatrixRoomInvite
import app.dapk.st.matrix.sync.RoomOverview as MatrixRoomOverview
import app.dapk.st.matrix.sync.SyncService.SyncEvent.Typing as MatrixTyping

fun MatrixRoomOverview.engine() = RoomOverview(
    this.roomId,
    this.roomCreationUtc,
    this.roomName,
    this.roomAvatarUrl,
    this.lastMessage?.engine(),
    this.isGroup,
    this.readMarker,
    this.isEncrypted
)

fun MatrixLastMessage.engine() = RoomOverview.LastMessage(
    this.content,
    this.utcTimestamp,
    this.author,
)

fun MatrixTyping.engine() = Typing(
    this.roomId,
    this.members,
)

fun LoginRequest.engine() = MatrixLoginRequest(
    this.userName,
    this.password,
    this.serverUrl
)

fun MatrixLoginResult.engine() = when (this) {
    is AuthService.LoginResult.Error -> LoginResult.Error(this.cause)
    AuthService.LoginResult.MissingWellKnown -> LoginResult.MissingWellKnown
    is AuthService.LoginResult.Success -> LoginResult.Success(this.userCredentials)
}

fun MatrixMe.engine() = Me(
    this.userId,
    this.displayName,
    this.avatarUrl,
    this.homeServerUrl,
)

fun MatrixRoomInvite.engine() = RoomInvite(
    this.from,
    this.roomId,
    this.inviteMeta.engine(),
)

fun InviteMeta.engine() = when (this) {
    InviteMeta.DirectMessage -> RoomInvite.InviteMeta.DirectMessage
    is InviteMeta.Room -> RoomInvite.InviteMeta.Room(this.roomName)
}

fun MatrixImportResult.engine() = when (this) {
    is MatrixImportResult.Error -> ImportResult.Error(
        when (val error = this.cause) {
            MatrixImportResult.Error.Type.InvalidFile -> ImportResult.Error.Type.InvalidFile
            MatrixImportResult.Error.Type.NoKeysFound -> ImportResult.Error.Type.NoKeysFound
            MatrixImportResult.Error.Type.UnableToOpenFile -> ImportResult.Error.Type.UnableToOpenFile
            MatrixImportResult.Error.Type.UnexpectedDecryptionOutput -> ImportResult.Error.Type.UnexpectedDecryptionOutput
            is MatrixImportResult.Error.Type.Unknown -> ImportResult.Error.Type.Unknown(error.cause)
        }
    )

    is MatrixImportResult.Success -> ImportResult.Success(this.roomIds, this.totalImportedKeysCount)
    is MatrixImportResult.Update -> ImportResult.Update(this.importedKeysCount)
}