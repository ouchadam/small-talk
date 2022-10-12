package fixture

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.engine.NotificationDiff

object NotificationDiffFixtures {

    fun aNotificationDiff(
        unchanged: Map<RoomId, List<EventId>> = emptyMap(),
        changedOrNew: Map<RoomId, List<EventId>> = emptyMap(),
        removed: Map<RoomId, List<EventId>> = emptyMap(),
        newRooms: Set<RoomId> = emptySet(),
    ) = NotificationDiff(unchanged, changedOrNew, removed, newRooms)

}