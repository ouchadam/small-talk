package app.dapk.st.push.firebase

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.core.log
import app.dapk.st.core.module
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.push.PushModule
import app.dapk.st.push.PushTokenPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"

class FirebasePushService : FirebaseMessagingService() {

    private val handler by unsafeLazy { module<PushModule>().pushHandler() }

    override fun onNewToken(token: String) {
        log(AppLogTag.PUSH, "FCM onNewToken")
        handler.onNewToken(
            PushTokenPayload(
                token = token,
                gatewayUrl = SYGNAL_GATEWAY,
            )
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        log(AppLogTag.PUSH, "FCM onMessage")
        val eventId = message.data["event_id"]?.let { EventId(it) }
        val roomId = message.data["room_id"]?.let { RoomId(it) }
        handler.onMessageReceived(eventId, roomId)
    }
}
