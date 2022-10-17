package app.dapk.st.notifications

import android.content.Context
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.RoomOverview
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.navigator.IntentFactory
import fixture.NotificationDelegateFixtures.anAndroidNotification
import fixture.NotificationFixtures.aDismissRoomNotification
import fixture.NotificationFixtures.aRoomNotification
import fixture.aRoomMessageEvent
import fixture.aRoomOverview
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn

private val A_SUMMARY_NOTIFICATION = anAndroidNotification()
private val A_ROOM_OVERVIEW = aRoomOverview()

class NotificationStateMapperTest {

    private val fakeRoomEventsToNotifiableMapper = FakeRoomEventsToNotifiableMapper()
    private val fakeNotificationFactory = FakeNotificationFactory()

    private val factory = NotificationStateMapper(
        fakeRoomEventsToNotifiableMapper.instance,
        fakeNotificationFactory.instance,
    )

    @Test
    fun `given no room message events, when mapping notifications, then creates doesn't create summary and dismisses rooms`() = runTest {
        val notificationState = aNotificationState(allUnread = mapOf(A_ROOM_OVERVIEW to listOf()))
        fakeRoomEventsToNotifiableMapper.given(emptyList()).returns(emptyList())

        val result = factory.mapToNotifications(notificationState)

        result shouldBeEqualTo Notifications(
            summaryNotification = null,
            delegates = listOf(aDismissRoomNotification(A_ROOM_OVERVIEW.roomId))
        )
    }

    @Test
    fun `given room message events, when mapping notifications, then creates summary and message notifications`() = runTest {
        val notificationState = aNotificationState(allUnread = mapOf(aRoomOverview() to listOf(aRoomMessageEvent())))
        val expectedNotification = givenCreatesNotification(notificationState, aRoomNotification())
        fakeNotificationFactory.givenCreateSummary(listOf(expectedNotification)).returns(A_SUMMARY_NOTIFICATION)

        val result = factory.mapToNotifications(notificationState)

        result shouldBeEqualTo Notifications(
            summaryNotification = A_SUMMARY_NOTIFICATION,
            delegates = listOf(expectedNotification)
        )

//
//        val allUnread = listOf(aRoomMessageEvent())
//        val value = listOf(aNotifiable())
//        fakeRoomEventsToNotifiableMapper.given(allUnread).returns(value)
//
//        fakeIntentFactory.notificationOpenApp()
//        fakeIntentFactory.notificationOpenMessage()
//
//        fakeNotificationStyleFactory.givenMessage(value, aRoomOverview()).returns(aMessagingStyle())
//        fakeNotificationStyleFactory.givenSummary(listOf()).returns(anInboxStyle())
//
//        val result = factory.mapToNotifications(
//            allUnread = mapOf(
//                aRoomOverview() to allUnread
//            ),
//            roomsWithNewEvents = setOf(),
//            newRooms = setOf()
//        )
//
//
//        result shouldBeEqualTo Notifications(
//            summaryNotification = anAndroidNotification(),
//            delegates = listOf(
//                NotificationTypes.Room(
//                    anAndroidNotification(),
//                    aRoomId(),
//                    summary = "a summary",
//                    messageCount = 1,
//                    isAlerting = false
//                )
//            )
//        )
    }

    private fun givenCreatesNotification(state: NotificationState, result: NotificationTypes.Room): NotificationTypes.Room {
        state.allUnread.map { (roomOverview, events) ->
            val value = listOf(aNotifiable())
            fakeRoomEventsToNotifiableMapper.given(events).returns(value)
            fakeNotificationFactory.givenCreateMessage(
                value,
                roomOverview,
                state.roomsWithNewEvents,
                state.newRooms
            ).returns(result)
        }
        return result
    }
}

class FakeIntentFactory : IntentFactory by mockk() {
    fun givenNotificationOpenApp(context: Context) = every { notificationOpenApp(context) }.delegateReturn()
    fun givenNotificationOpenMessage(context: Context, roomId: RoomId) = every { notificationOpenMessage(context, roomId) }.delegateReturn()
}

class FakeNotificationStyleFactory {
    val instance = mockk<NotificationStyleFactory>()

    fun givenMessage(events: List<Notifiable>, roomOverview: RoomOverview) = coEvery {
        instance.message(events, roomOverview)
    }.delegateReturn()

    fun givenSummary(notifications: List<NotificationTypes.Room>) = every { instance.summary(notifications) }.delegateReturn()

}

class FakeRoomEventsToNotifiableMapper {
    val instance = mockk<RoomEventsToNotifiableMapper>()

    fun given(events: List<RoomEvent>) = every { instance.map(events) }.delegateReturn()
}

class FakeNotificationFactory {
    val instance = mockk<NotificationFactory>()

    fun givenCreateMessage(
        events: List<Notifiable>,
        roomOverview: RoomOverview,
        roomsWithNewEvents: Set<RoomId>,
        newRooms: Set<RoomId>
    ) = coEvery { instance.createMessageNotification(events, roomOverview, roomsWithNewEvents, newRooms) }.delegateReturn()

    fun givenCreateSummary(roomNotifications: List<NotificationTypes.Room>) = every { instance.createSummary(roomNotifications) }.delegateReturn()
}