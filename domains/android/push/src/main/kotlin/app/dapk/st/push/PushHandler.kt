package app.dapk.st.push

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId

interface PushHandler {
    fun onNewToken(token: String)
    fun onMessageReceived(eventId: EventId?, roomId: RoomId?)
}