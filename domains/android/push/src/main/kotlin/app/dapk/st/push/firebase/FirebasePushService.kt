package app.dapk.st.push.firebase

import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.core.module
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.push.PushModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebasePushService : FirebaseMessagingService() {

    private val handler by unsafeLazy { module<PushModule>().pushHandler() }

    override fun onNewToken(token: String) {
        handler.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val eventId = message.data["event_id"]?.let { EventId(it) }
        val roomId = message.data["room_id"]?.let { RoomId(it) }
        handler.onMessageReceived(eventId, roomId)
    }
}
