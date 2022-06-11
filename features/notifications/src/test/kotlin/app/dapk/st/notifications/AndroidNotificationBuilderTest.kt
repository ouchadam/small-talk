package app.dapk.st.notifications

import app.dapk.st.core.DeviceMeta
import fixture.NotificationDelegateFixtures.anAndroidNotification
import fake.FakeContext
import fake.FakeNotificationBuilder
import fake.aFakeMessagingStyle
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private val A_MESSAGING_STYLE = aFakeMessagingStyle()

class AndroidNotificationBuilderTest {

    private val fakeContext = FakeContext()
    private val fakeNotificationBuilder = FakeNotificationBuilder()
    private val fakeAndroidNotificationStyleBuilder = FakeAndroidNotificationStyleBuilder()

    private val builder = AndroidNotificationBuilder(
        fakeContext.instance,
        DeviceMeta(apiVersion = 26),
        fakeAndroidNotificationStyleBuilder.instance,
        builderFactory = { _, _, _ -> fakeNotificationBuilder.instance },
    )

    @Test
    fun `applies all builder options`() = runExpectTest {
        val notification = anAndroidNotification()
        fakeAndroidNotificationStyleBuilder.given(notification.messageStyle!!).returns(A_MESSAGING_STYLE)
        fakeNotificationBuilder.instance.captureExpects {
            it.setOnlyAlertOnce(!notification.alertMoreThanOnce)
            it.setAutoCancel(notification.autoCancel)
            it.setGroupSummary(notification.isGroupSummary)
            it.setGroup(notification.groupId)
            it.setStyle(A_MESSAGING_STYLE)
            it.setContentIntent(notification.contentIntent)
            it.setShowWhen(true)
            it.setWhen(notification.whenTimestamp!!)
            it.setCategory(notification.category)
            it.setShortcutId(notification.shortcutId)
            it.setSmallIcon(notification.smallIcon!!)
            it.setLargeIcon(notification.largeIcon)
            it.build()
        }

        val ignoredResult = builder.build(notification)

        verifyExpects()
    }
}

class FakeAndroidNotificationStyleBuilder {
    val instance = mockk<AndroidNotificationStyleBuilder>()

    fun given(style: AndroidNotificationStyle) = every { instance.build(style) }.delegateReturn()
}
