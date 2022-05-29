package app.dapk.st.notifications

import app.dapk.st.notifications.NotificationFixtures.aNotifications
import fake.FakeNotificationFactory
import fake.FakeNotificationManager
import fake.aFakeNotification
import fixture.CoroutineDispatchersFixture.aCoroutineDispatchers
import fixture.aRoomId
import org.junit.Test
import test.expect
import test.runExpectTest

private const val SUMMARY_ID = 101
private const val ROOM_MESSAGE_ID = 100
private val A_SUMMARY_NOTIFICATION = aFakeNotification()

class NotificationRendererTest {

    private val fakeNotificationManager = FakeNotificationManager()
    private val fakeNotificationFactory = FakeNotificationFactory()

    private val notificationRenderer = NotificationRenderer(
        fakeNotificationManager.instance,
        fakeNotificationFactory.instance,
        aCoroutineDispatchers()
    )

    @Test
    fun `given removed rooms when rendering then cancels notifications with cancelled room ids`() = runExpectTest {
        val removedRooms = setOf(aRoomId("id-1"), aRoomId("id-2"))
        fakeNotificationFactory.instance.expect { it.createNotifications(emptyMap(), emptySet(), emptySet()) }
        fakeNotificationManager.instance.expectUnit {
            removedRooms.forEach { removedRoom -> it.cancel(removedRoom.value, ROOM_MESSAGE_ID) }
        }

        notificationRenderer.render(emptyMap(), removedRooms, emptySet(), emptySet())

        verifyExpects()
    }

    @Test
    fun `given summary notification is not created, when rendering, then cancels summary notification`() = runExpectTest {
        fakeNotificationFactory.givenNotifications(emptyMap(), emptySet(), emptySet()).returns(aNotifications(summaryNotification = null))
        fakeNotificationManager.instance.expectUnit { it.cancel(SUMMARY_ID) }

        notificationRenderer.render(emptyMap(), emptySet(), emptySet(), emptySet())

        verifyExpects()
    }

    @Test
    fun `given update is only removals, when rendering, then only renders room dismiss`() = runExpectTest {
        fakeNotificationFactory.givenNotifications(emptyMap(), emptySet(), emptySet()).returns(aNotifications(summaryNotification = null))
        fakeNotificationManager.instance.expectUnit { it.cancel(SUMMARY_ID) }

        notificationRenderer.render(emptyMap(), emptySet(), emptySet(), emptySet())

        verifyExpects()
    }

    @Test
    fun `given rooms with events, when rendering, then notifies summary and new rooms`() = runExpectTest {
        val roomNotification = aRoomNotification()
        val roomsWithNewEvents = setOf(roomNotification.roomId)
        fakeNotificationFactory.givenNotifications(emptyMap(), roomsWithNewEvents, emptySet()).returns(
            aNotifications(summaryNotification = A_SUMMARY_NOTIFICATION, delegates = listOf(roomNotification))
        )
        fakeNotificationManager.instance.expectUnit { it.notify(SUMMARY_ID, A_SUMMARY_NOTIFICATION) }
        fakeNotificationManager.instance.expectUnit { it.notify(roomNotification.roomId.value, ROOM_MESSAGE_ID, roomNotification.notification) }

        notificationRenderer.render(emptyMap(), emptySet(), roomsWithNewEvents, emptySet())

        verifyExpects()
    }

    private fun aRoomNotification() = NotificationDelegate.Room(
        aFakeNotification(),
        aRoomId(),
        "a summary line",
        messageCount = 1,
        isAlerting = false
    )
}


