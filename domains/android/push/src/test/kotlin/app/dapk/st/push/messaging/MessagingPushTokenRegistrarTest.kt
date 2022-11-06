package app.dapk.st.push.messaging

import app.dapk.st.firebase.messaging.Messaging
import app.dapk.st.push.PushTokenPayload
import app.dapk.st.push.unifiedpush.FakePushHandler
import fake.FakeErrorTracker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private const val A_TOKEN = "a-token"
private const val SYGNAL_GATEWAY = "https://sygnal.dapk.app/_matrix/push/v1/notify"
private val AN_ERROR = RuntimeException()

class MessagingPushTokenRegistrarTest {

    private val fakePushHandler = FakePushHandler()
    private val fakeErrorTracker = FakeErrorTracker()
    private val fakeMessaging = FakeMessaging()

    private val registrar = MessagingPushTokenRegistrar(
        fakeErrorTracker,
        fakePushHandler,
        fakeMessaging.instance,
    )

    @Test
    fun `when checking isAvailable, then delegates`() = runExpectTest {
        fakeMessaging.givenIsAvailable().returns(true)

        val result = registrar.isAvailable()

        result shouldBeEqualTo true
    }

    @Test
    fun `when registering current token, then enables and forwards current token to handler`() = runExpectTest {
        fakeMessaging.instance.expect { it.enable() }
        fakePushHandler.expect { it.onNewToken(PushTokenPayload(A_TOKEN, SYGNAL_GATEWAY)) }
        fakeMessaging.givenToken().returns(A_TOKEN)

        registrar.registerCurrentToken()

        verifyExpects()
    }

    @Test
    fun `given fails to register, when registering current token, then tracks error`() = runExpectTest {
        fakeMessaging.instance.expect { it.enable() }
        fakeMessaging.givenToken().throws(AN_ERROR)
        fakeErrorTracker.expect { it.track(AN_ERROR) }

        registrar.registerCurrentToken()

        verifyExpects()
    }

    @Test
    fun `when unregistering, then deletes token and disables`() = runExpectTest {
        fakeMessaging.instance.expect { it.deleteToken() }
        fakeMessaging.instance.expect { it.disable() }

        registrar.unregister()

        verifyExpects()
    }

}

class FakeMessaging {
    val instance = mockk<Messaging>()

    fun givenIsAvailable() = every { instance.isAvailable() }.delegateReturn()
    fun givenToken() = coEvery { instance.token() }.delegateReturn()
}