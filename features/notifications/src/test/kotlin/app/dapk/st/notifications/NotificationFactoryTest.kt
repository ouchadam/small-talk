package app.dapk.st.notifications

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import app.dapk.st.core.DeviceMeta
import app.dapk.st.engine.RoomOverview
import app.dapk.st.matrix.common.AvatarUrl
import fake.FakeContext
import fixture.NotificationDelegateFixtures.anAndroidNotification
import fixture.NotificationDelegateFixtures.anInboxStyle
import fixture.NotificationFixtures.aRoomNotification
import fixture.aRoomId
import fixture.aRoomOverview
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val A_CHANNEL_ID = "a channel id"
private val AN_OPEN_APP_INTENT = aPendingIntent()
private val AN_OPEN_ROOM_INTENT = aPendingIntent()
private val A_NOTIFICATION_STYLE = anInboxStyle()
private val A_ROOM_ID = aRoomId()
private val A_DM_ROOM_OVERVIEW = aRoomOverview(roomId = A_ROOM_ID, roomAvatarUrl = AvatarUrl("https://a-url.gif"), isGroup = false)
private val A_GROUP_ROOM_OVERVIEW = aRoomOverview(roomId = A_ROOM_ID, roomAvatarUrl = AvatarUrl("https://a-url.gif"), isGroup = true)
private val A_ROOM_ICON = anIcon()
private val LATEST_EVENT = aNotifiable("message three", utcTimestamp = 3)
private val EVENTS = listOf(
    aNotifiable("message one", utcTimestamp = 1),
    LATEST_EVENT,
    aNotifiable("message two", utcTimestamp = 2),
)

class NotificationFactoryTest {

    private val fakeContext = FakeContext()
    private val fakeNotificationStyleFactory = FakeNotificationStyleFactory()
    private val fakeIntentFactory = FakeIntentFactory()
    private val fakeIconLoader = FakeIconLoader()
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC)

    private val notificationFactory = NotificationFactory(
        fakeContext.instance,
        fakeNotificationStyleFactory.instance,
        fakeIntentFactory,
        fakeIconLoader,
        DeviceMeta(26),
        fixedClock
    )

    @Test
    fun `given alerting room notification, when creating summary, then is alerting`() {
        val notifications = listOf(
            aRoomNotification(
                summaryChannelId = A_CHANNEL_ID,
                notification = anAndroidNotification(channelId = A_CHANNEL_ID), isAlerting = true
            )
        )
        fakeIntentFactory.givenNotificationOpenApp(fakeContext.instance).returns(AN_OPEN_APP_INTENT)
        fakeNotificationStyleFactory.givenSummary(notifications).returns(anInboxStyle())

        val result = notificationFactory.createSummary(notifications)

        result shouldBeEqualTo expectedSummary(notifications.first().notification, shouldAlertMoreThanOnce = true)
    }

    @Test
    fun `given non alerting room notification, when creating summary, then is alerting`() {
        val notifications = listOf(
            aRoomNotification(
                summaryChannelId = A_CHANNEL_ID,
                notification = anAndroidNotification(channelId = A_CHANNEL_ID), isAlerting = false
            )
        )
        fakeIntentFactory.givenNotificationOpenApp(fakeContext.instance).returns(AN_OPEN_APP_INTENT)
        fakeNotificationStyleFactory.givenSummary(notifications).returns(anInboxStyle())

        val result = notificationFactory.createSummary(notifications)

        result shouldBeEqualTo expectedSummary(notifications.first().notification, shouldAlertMoreThanOnce = false)
    }

    @Test
    fun `given new events in a new group room, when creating message, then alerts`() = runTest {
        givenEventsFor(A_GROUP_ROOM_OVERVIEW)

        val result = notificationFactory.createMessageNotification(EVENTS, A_GROUP_ROOM_OVERVIEW, setOf(A_ROOM_ID), newRooms = setOf(A_ROOM_ID))

        result shouldBeEqualTo expectedMessage(
            channel = GROUP_CHANNEL_ID,
            shouldAlertMoreThanOnce = true,
        )
    }

    @Test
    fun `given new events in an existing group room, when creating message, then does not alert`() = runTest {
        givenEventsFor(A_GROUP_ROOM_OVERVIEW)

        val result = notificationFactory.createMessageNotification(EVENTS, A_GROUP_ROOM_OVERVIEW, setOf(A_ROOM_ID), newRooms = emptySet())

        result shouldBeEqualTo expectedMessage(
            channel = GROUP_CHANNEL_ID,
            shouldAlertMoreThanOnce = false,
        )
    }

    @Test
    fun `given new events in a new DM room, when creating message, then alerts`() = runTest {
        givenEventsFor(A_DM_ROOM_OVERVIEW)

        val result = notificationFactory.createMessageNotification(EVENTS, A_DM_ROOM_OVERVIEW, setOf(A_ROOM_ID), newRooms = setOf(A_ROOM_ID))

        result shouldBeEqualTo expectedMessage(
            channel = DIRECT_CHANNEL_ID,
            shouldAlertMoreThanOnce = true,
        )
    }

    @Test
    fun `given new events in an existing DM room, when creating message, then alerts`() = runTest {
        givenEventsFor(A_DM_ROOM_OVERVIEW)

        val result = notificationFactory.createMessageNotification(EVENTS, A_DM_ROOM_OVERVIEW, setOf(A_ROOM_ID), newRooms = emptySet())

        result shouldBeEqualTo expectedMessage(
            channel = DIRECT_CHANNEL_ID,
            shouldAlertMoreThanOnce = true,
        )
    }

    @Test
    fun `given invite, then creates expected`() {
        fakeIntentFactory.givenNotificationOpenApp(fakeContext.instance).returns(AN_OPEN_APP_INTENT)
        val content = "Content message"
        val result = notificationFactory.createInvite(
            app.dapk.st.engine.InviteNotification(
                content = content,
                A_ROOM_ID,
            )
        )

        result shouldBeEqualTo AndroidNotification(
            channelId = INVITE_CHANNEL_ID,
            whenTimestamp = fixedClock.millis(),
            alertMoreThanOnce = true,
            smallIcon = R.drawable.ic_notification_small_icon,
            contentIntent = AN_OPEN_APP_INTENT,
            category = Notification.CATEGORY_EVENT,
            autoCancel = true,
            contentTitle = "Invite",
            contentText = content,
        )
    }

    private fun givenEventsFor(roomOverview: RoomOverview) {
        fakeIntentFactory.givenNotificationOpenMessage(fakeContext.instance, roomOverview.roomId).returns(AN_OPEN_ROOM_INTENT)
        fakeNotificationStyleFactory.givenMessage(EVENTS.sortedBy { it.utcTimestamp }, roomOverview).returns(A_NOTIFICATION_STYLE)
        fakeIconLoader.given(roomOverview.roomAvatarUrl!!.value).returns(A_ROOM_ICON)
    }

    private fun expectedMessage(
        channel: String,
        shouldAlertMoreThanOnce: Boolean,
    ) = NotificationTypes.Room(
        AndroidNotification(
            channelId = SUMMARY_CHANNEL_ID,
            whenTimestamp = LATEST_EVENT.utcTimestamp,
            groupId = "st",
            groupAlertBehavior = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.GROUP_ALERT_SUMMARY else null,
            shortcutId = A_ROOM_ID.value,
            alertMoreThanOnce = false,
            contentIntent = AN_OPEN_ROOM_INTENT,
            messageStyle = A_NOTIFICATION_STYLE,
            category = Notification.CATEGORY_MESSAGE,
            smallIcon = R.drawable.ic_notification_small_icon,
            largeIcon = A_ROOM_ICON,
            autoCancel = true
        ),
        A_ROOM_ID,
        summary = LATEST_EVENT.content,
        messageCount = EVENTS.size,
        isAlerting = shouldAlertMoreThanOnce,
        summaryChannelId = channel,
    )

    private fun expectedSummary(notification: AndroidNotification, shouldAlertMoreThanOnce: Boolean) = AndroidNotification(
        channelId = notification.channelId,
        whenTimestamp = notification.whenTimestamp,
        messageStyle = A_NOTIFICATION_STYLE,
        alertMoreThanOnce = shouldAlertMoreThanOnce,
        smallIcon = R.drawable.ic_notification_small_icon,
        contentIntent = AN_OPEN_APP_INTENT,
        groupId = "st",
        category = Notification.CATEGORY_MESSAGE,
        isGroupSummary = true,
        autoCancel = true
    )
}

fun aPendingIntent() = mockk<PendingIntent>()