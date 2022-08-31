package app.dapk.st.firebase.messaging

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId

interface ServiceDelegate {
    fun onNewToken(token: String)
    fun onMessageReceived(eventId: EventId?, roomId: RoomId?)
}