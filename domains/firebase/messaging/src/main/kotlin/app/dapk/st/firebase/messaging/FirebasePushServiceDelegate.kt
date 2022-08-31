package app.dapk.st.firebase.messaging

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.core.log
import app.dapk.st.core.module
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebasePushServiceDelegate : FirebaseMessagingService() {

    private val delegate by unsafeLazy { module<MessagingModule>().serviceDelegate }

    override fun onNewToken(token: String) {
        delegate.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        log(AppLogTag.PUSH, "FCM onMessage")
        val eventId = message.data["event_id"]?.let { EventId(it) }
        val roomId = message.data["room_id"]?.let { RoomId(it) }
        delegate.onMessageReceived(eventId, roomId)
    }
}
