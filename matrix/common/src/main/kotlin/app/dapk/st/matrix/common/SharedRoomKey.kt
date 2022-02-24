package app.dapk.st.matrix.common

data class SharedRoomKey(
    val algorithmName: AlgorithmName,
    val roomId: RoomId,
    val sessionId: SessionId,
    val sessionKey: String,
    val isExported: Boolean,
)