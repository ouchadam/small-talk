package app.dapk.st.push.unifiedpush

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.push.PushModule
import app.dapk.st.push.PushTokenPayload
import fake.FakeContext
import fixture.CoroutineDispatchersFixture.aCoroutineDispatchers
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import test.delegateReturn
import test.runExpectTest
import java.net.URL

private val A_CONTEXT = FakeContext()
private const val A_ROOM_ID = "a room id"
private const val AN_EVENT_ID = "an event id"
private const val AN_ENDPOINT_HOST = "https://aendpointurl.com"
private const val AN_ENDPOINT = "$AN_ENDPOINT_HOST/with/path"
private const val A_GATEWAY_URL = "$AN_ENDPOINT_HOST/_matrix/push/v1/notify"
private const val FALLBACK_GATEWAY_URL = "https://matrix.gateway.unifiedpush.org/_matrix/push/v1/notify"

class UnifiedPushMessageDelegateTest {

    private val fakePushHandler = FakePushHandler()
    private val fakeEndpointReader = FakeEndpointReader()
    private val fakePushModule = FakePushModule().also {
        it.givenPushHandler().returns(fakePushHandler)
    }

    private val unifiedPushReceiver = UnifiedPushMessageDelegate(
        CoroutineScope(UnconfinedTestDispatcher()),
        pushModuleProvider = { _ -> fakePushModule.instance },
        endpointReader = fakeEndpointReader,
    )

    @Test
    fun `parses incoming message payloads`() = runExpectTest {
        fakePushHandler.expect { it.onMessageReceived(EventId(AN_EVENT_ID), RoomId(A_ROOM_ID)) }
        val messageBytes = createMessage(A_ROOM_ID, AN_EVENT_ID)

        unifiedPushReceiver.onMessage(A_CONTEXT.instance, messageBytes)

        verifyExpects()
    }

    @Test
    fun `given endpoint is a gateway, then uses original endpoint url`() = runExpectTest {
        fakeEndpointReader.given(A_GATEWAY_URL).returns("""{"unifiedpush":{"gateway":"matrix"}}""")
        fakePushHandler.expect { it.onNewToken(PushTokenPayload(token = AN_ENDPOINT, gatewayUrl = A_GATEWAY_URL)) }

        unifiedPushReceiver.onNewEndpoint(A_CONTEXT.instance, AN_ENDPOINT)

        verifyExpects()
    }

    @Test
    fun `given endpoint is not a gateway, then uses fallback endpoint url`() = runExpectTest {
        fakeEndpointReader.given(A_GATEWAY_URL).returns("")
        fakePushHandler.expect { it.onNewToken(PushTokenPayload(token = AN_ENDPOINT, gatewayUrl = FALLBACK_GATEWAY_URL)) }

        unifiedPushReceiver.onNewEndpoint(A_CONTEXT.instance, AN_ENDPOINT)

        verifyExpects()
    }

    private fun createMessage(roomId: String, eventId: String) = """
            {
                "notification": {
                    "room_id": "$roomId",
                    "event_id": "$eventId"
                }
            }
        """.trimIndent().toByteArray()
}

class FakePushModule {
    val instance = mockk<PushModule>()

    init {
        every { instance.dispatcher() }.returns(aCoroutineDispatchers())
    }

    fun givenPushHandler() = every { instance.pushHandler() }.delegateReturn()
}

class FakeEndpointReader : suspend (URL) -> String by mockk() {

    fun given(url: String) = coEvery { this@FakeEndpointReader.invoke(URL(url)) }.delegateReturn()

}