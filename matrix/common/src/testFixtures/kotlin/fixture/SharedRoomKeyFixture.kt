package fixture

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SessionId
import app.dapk.st.matrix.common.SharedRoomKey

fun aSharedRoomKey(
    algorithmName: AlgorithmName = anAlgorithmName(),
    roomId: RoomId = aRoomId(),
    sessionId: SessionId = aSessionId(),
    sessionKey: String = "a-session-key",
    isExported: Boolean = false,
) = SharedRoomKey(algorithmName, roomId, sessionId, sessionKey, isExported)