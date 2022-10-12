package fixture

import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomState

fun aMatrixRoomState(
    roomOverview: RoomOverview = aMatrixRoomOverview(),
    events: List<RoomEvent> = listOf(aMatrixRoomMessageEvent()),
) = RoomState(roomOverview, events)