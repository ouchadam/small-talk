package fixture

import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.RoomState

fun aRoomState(
    roomOverview: RoomOverview = aMatrixRoomOverview(),
    events: List<RoomEvent> = listOf(aRoomMessageEvent()),
) = RoomState(roomOverview, events)