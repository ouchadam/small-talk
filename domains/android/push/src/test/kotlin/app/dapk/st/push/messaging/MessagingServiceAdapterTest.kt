package app.dapk.st.push.messaging

import app.dapk.st.push.PushTokenPayload
import app.dapk.st.push.unifiedpush.FakePushHandler
import fixture.aRoomId
import fixture.anEventId
import org.junit.Test
import test.runExpectTest

private const val A_TOKEN = "a-push-token"
private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"
private val A_ROOM_ID = aRoomId()
private val AN_EVENT_ID = anEventId()

class MessagingServiceAdapterTest {

    private val fakePushHandler = FakePushHandler()

    private val messagingServiceAdapter = MessagingServiceAdapter(fakePushHandler)

    @Test
    fun `onNewToken, then delegates to push handler`() = runExpectTest {
        fakePushHandler.expect {
            it.onNewToken(PushTokenPayload(token = A_TOKEN, gatewayUrl = SYGNAL_GATEWAY))
        }
        messagingServiceAdapter.onNewToken(A_TOKEN)

        verifyExpects()
    }


    @Test
    fun `onMessageReceived, then delegates to push handler`() = runExpectTest {
        fakePushHandler.expect {
            it.onMessageReceived(AN_EVENT_ID, A_ROOM_ID)
        }
        messagingServiceAdapter.onMessageReceived(AN_EVENT_ID, A_ROOM_ID)

        verifyExpects()
    }
}
