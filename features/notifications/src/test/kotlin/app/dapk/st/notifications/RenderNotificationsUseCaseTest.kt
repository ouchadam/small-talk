package app.dapk.st.notifications

import fake.FakeNotificationChannels
import fake.FakeNotificationRenderer
import fake.FakeObserveUnreadNotificationsUseCase
import fixture.NotificationDiffFixtures.aNotificationDiff
import kotlinx.coroutines.test.runTest
import org.junit.Test
import test.expect

private val AN_UNREAD_NOTIFICATIONS = UnreadNotifications(emptyMap(), aNotificationDiff())

class RenderNotificationsUseCaseTest {

    private val fakeNotificationRenderer = FakeNotificationRenderer()
    private val fakeObserveUnreadNotificationsUseCase = FakeObserveUnreadNotificationsUseCase()
    private val fakeNotificationChannels = FakeNotificationChannels().also {
        it.instance.expect { it.initChannels() }
    }

    private val renderNotificationsUseCase = RenderNotificationsUseCase(
        fakeNotificationRenderer.instance,
        fakeObserveUnreadNotificationsUseCase,
        fakeNotificationChannels.instance,
    )

    @Test
    fun `given events, when listening for changes then initiates channels once`() = runTest {
        fakeNotificationRenderer.instance.expect { it.render(any()) }
        fakeObserveUnreadNotificationsUseCase.given().emits(AN_UNREAD_NOTIFICATIONS)

        renderNotificationsUseCase.listenForNotificationChanges()

        fakeNotificationChannels.verifyInitiated()
    }

    @Test
    fun `given renderable unread events, when listening for changes, then renders change`() = runTest {
        fakeNotificationRenderer.instance.expect { it.render(any()) }
        fakeObserveUnreadNotificationsUseCase.given().emits(AN_UNREAD_NOTIFICATIONS)

        renderNotificationsUseCase.listenForNotificationChanges()

        fakeNotificationRenderer.verifyRenders(AN_UNREAD_NOTIFICATIONS)
    }
}
