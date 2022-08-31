package app.dapk.st.push.messaging

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import app.dapk.st.firebase.messaging.ServiceDelegate
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.push.PushHandler
import app.dapk.st.push.PushTokenPayload

private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"

class MessagingServiceAdapter(
    private val handler: PushHandler,
) : ServiceDelegate {

    override fun onNewToken(token: String) {
        log(AppLogTag.PUSH, "FCM onNewToken")
        handler.onNewToken(
            PushTokenPayload(
                token = token,
                gatewayUrl = SYGNAL_GATEWAY,
            )
        )
    }

    override fun onMessageReceived(eventId: EventId?, roomId: RoomId?) {
        log(AppLogTag.PUSH, "FCM onMessage")
        handler.onMessageReceived(eventId, roomId)
    }
}
