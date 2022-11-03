package app.dapk.st.notifications

import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.RoomOverview
import app.dapk.st.matrix.common.RoomId
import fake.FakeNotificationFactory
import fake.FakeNotificationManager
import fake.aFakeNotification
import fixture.CoroutineDispatchersFixture.aCoroutineDispatchers
import fixture.NotificationDelegateFixtures.anAndroidNotification
import fixture.NotificationFixtures.aNotifications
import fixture.NotificationFixtures.aRoomNotification
import fixture.aRoomId
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import test.delegateReturn
import test.runExpectTest

private const val SUMMARY_ID = 101
private const val ROOM_MESSAGE_ID = 100
private val A_SUMMARY_ANDROID_NOTIFICATION = anAndroidNotification(isGroupSummary = true)
private val A_NOTIFICATION = aFakeNotification()

class FakeAndroidNotificationBuilder {
    val instance = mockk<AndroidNotificationBuilder>()

    fun given(notification: AndroidNotification) = every { instance.build(notification) }.delegateReturn()
}

class NotificationRendererTest {

    private val fakeNotificationManager = FakeNotificationManager()
    private val fakeNotificationFactory = FakeNotificationFactory()
    private val fakeAndroidNotificationBuilder = FakeAndroidNotificationBuilder()

    private val notificationRenderer = NotificationMessageRenderer(
        fakeNotificationManager.instance,
        fakeNotificationFactory.instance,
        fakeAndroidNotificationBuilder.instance,
        aCoroutineDispatchers()
    )

    @Test
    fun `given removed rooms when rendering then cancels notifications with cancelled room ids`() = runExpectTest {
        val removedRooms = setOf(aRoomId("id-1"), aRoomId("id-2"))
        val state = aNotificationState(removedRooms = removedRooms)
        fakeNotificationFactory.givenNotifications(state).returns(aNotifications())
        fakeNotificationManager.instance.expectUnit { removedRooms.forEach { removedRoom -> it.cancel(removedRoom.value, ROOM_MESSAGE_ID) } }
        fakeNotificationManager.instance.expectUnit { it.cancel(SUMMARY_ID) }

        notificationRenderer.render(state)

        verifyExpects()
    }

    @Test
    fun `given summary notification is not created, when rendering, then cancels summary notification`() = runExpectTest {
        fakeNotificationFactory.givenNotifications(aNotificationState()).returns(aNotifications(summaryNotification = null))
        fakeNotificationManager.instance.expectUnit { it.cancel(SUMMARY_ID) }

        notificationRenderer.render(NotificationState(emptyMap(), emptySet(), emptySet(), emptySet()))

        verifyExpects()
    }

    @Test
    fun `given update is only removals, when rendering, then only renders room dismiss`() = runExpectTest {
        fakeNotificationFactory.givenNotifications(aNotificationState()).returns(aNotifications(summaryNotification = null))
        fakeNotificationManager.instance.expectUnit { it.cancel(SUMMARY_ID) }

        notificationRenderer.render(NotificationState(emptyMap(), emptySet(), emptySet(), emptySet()))

        verifyExpects()
    }

    @Test
    fun `given rooms with events, when rendering, then notifies summary and new rooms`() = runExpectTest {
        val roomNotification = aRoomNotification()
        val roomsWithNewEvents = setOf(roomNotification.roomId)

        fakeAndroidNotificationBuilder.given(roomNotification.notification).returns(A_NOTIFICATION)
        fakeAndroidNotificationBuilder.given(A_SUMMARY_ANDROID_NOTIFICATION).returns(A_NOTIFICATION)

        fakeNotificationFactory.givenNotifications(aNotificationState(roomsWithNewEvents = roomsWithNewEvents)).returns(
            aNotifications(summaryNotification = A_SUMMARY_ANDROID_NOTIFICATION, delegates = listOf(roomNotification))
        )
        fakeNotificationManager.instance.expectUnit { it.notify(SUMMARY_ID, A_NOTIFICATION) }
        fakeNotificationManager.instance.expectUnit { it.notify(roomNotification.roomId.value, ROOM_MESSAGE_ID, A_NOTIFICATION) }

        notificationRenderer.render(NotificationState(emptyMap(), emptySet(), roomsWithNewEvents, emptySet()))

        verifyExpects()
    }
}

fun aNotificationState(
    allUnread: Map<RoomOverview, List<RoomEvent>> = emptyMap(),
    removedRooms: Set<RoomId> = emptySet(),
    roomsWithNewEvents: Set<RoomId> = emptySet(),
    newRooms: Set<RoomId> = emptySet(),
) = NotificationState(allUnread, removedRooms, roomsWithNewEvents, newRooms)

