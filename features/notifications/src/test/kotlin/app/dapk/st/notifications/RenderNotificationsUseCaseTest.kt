package app.dapk.st.notifications

import app.dapk.st.engine.UnreadNotifications
import fake.*
import fixture.NotificationDiffFixtures.aNotificationDiff
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.expect

private val AN_UNREAD_NOTIFICATIONS = UnreadNotifications(emptyMap(), aNotificationDiff())

class RenderNotificationsUseCaseTest {

    private val fakeNotificationMessageRenderer = FakeNotificationMessageRenderer()
    private val fakeNotificationInviteRenderer = FakeNotificationInviteRenderer()
    private val fakeNotificationChannels = FakeNotificationChannels().also {
        it.instance.expect { it.initChannels() }
    }
    private val fakeChatEngine = FakeChatEngine()

    private val renderNotificationsUseCase = RenderNotificationsUseCase(
        fakeNotificationMessageRenderer.instance,
        fakeNotificationInviteRenderer.instance,
        fakeChatEngine,
        fakeNotificationChannels.instance,
    )

    @Test
    fun `given events, when listening for changes then initiates channels once`() = runTest {
        fakeNotificationMessageRenderer.instance.expect { it.render(any()) }
        fakeChatEngine.givenNotificationsMessages().emits(AN_UNREAD_NOTIFICATIONS)
        fakeChatEngine.givenNotificationsInvites().emits()

        renderNotificationsUseCase.listenForNotificationChanges(TestScope(UnconfinedTestDispatcher()))

        fakeNotificationChannels.verifyInitiated()
    }

    @Test
    fun `given renderable unread events, when listening for changes, then renders change`() = runTest {
        fakeNotificationMessageRenderer.instance.expect { it.render(any()) }
        fakeChatEngine.givenNotificationsMessages().emits(AN_UNREAD_NOTIFICATIONS)
        fakeChatEngine.givenNotificationsInvites().emits()

        renderNotificationsUseCase.listenForNotificationChanges(TestScope(UnconfinedTestDispatcher()))

        fakeNotificationMessageRenderer.verifyRenders(AN_UNREAD_NOTIFICATIONS)
    }
}
