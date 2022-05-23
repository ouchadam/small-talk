package app.dapk.st.notifications

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomOverview
import fake.FakeRoomStore
import fixture.aRoomId
import fixture.aRoomMessageEvent
import fixture.aRoomOverview
import fixture.anEventId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val NO_UNREADS = emptyMap<RoomOverview, List<RoomEvent>>()
val A_MESSAGE = aRoomMessageEvent(eventId = anEventId("1"), content = "hello")
val A_MESSAGE_2 = aRoomMessageEvent(eventId = anEventId("2"), content = "world")
val A_ROOM_OVERVIEW = aRoomOverview(roomId = aRoomId("1"))
val A_ROOM_OVERVIEW_2 = aRoomOverview(roomId = aRoomId("2"))

class ObserveUnreadNotificationsUseCaseTest {

    private val fakeRoomStore = FakeRoomStore()

    private val useCase = ObserveUnreadNotificationsUseCaseImpl(fakeRoomStore)

    @Test
    fun `given no initial unreads, when receiving new message, then emits message`() = runTest {
        givenNoInitialUnreads(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE) to aNotificationDiff(
                changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE),
                newRooms = setOf(A_ROOM_OVERVIEW.roomId)
            )
        )
    }

    @Test
    fun `given no initial unreads, when receiving multiple messages, then emits messages`() = runTest {
        givenNoInitialUnreads(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2))

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE) to aNotificationDiff(
                changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE),
                newRooms = setOf(A_ROOM_OVERVIEW.roomId)
            ),
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2) to aNotificationDiff(changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE, A_MESSAGE_2))
        )
    }

    @Test
    fun `given initial unreads, when receiving new message, then emits all messages`() = runTest {
        fakeRoomStore.givenUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2) to aNotificationDiff(changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE, A_MESSAGE_2))
        )
    }

    @Test
    fun `given initial unreads, when reading a message, then emits nothing`() = runTest {
        fakeRoomStore.givenUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE) + A_ROOM_OVERVIEW_2.withUnreads(A_MESSAGE_2), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo emptyList()
    }

    @Test
    fun `given initial unreads, when reading a duplicate unread, then emits nothing`() = runTest {
        fakeRoomStore.givenUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo emptyList()
    }

    private fun givenNoInitialUnreads(vararg unreads: Map<RoomOverview, List<RoomEvent>>) = fakeRoomStore.givenUnreadEvents(flowOf(NO_UNREADS, *unreads))
}

private fun aNotificationDiff(
    unchanged: Map<RoomId, List<EventId>> = emptyMap(),
    changedOrNew: Map<RoomId, List<EventId>> = emptyMap(),
    removed: Map<RoomId, List<EventId>> = emptyMap(),
    newRooms: Set<RoomId> = emptySet(),
) = NotificationDiff(unchanged, changedOrNew, removed, newRooms)

private fun RoomOverview.withUnreads(vararg events: RoomEvent) = mapOf(this to events.toList())
private fun RoomOverview.toDiff(vararg events: RoomEvent) = mapOf(this.roomId to events.map { it.eventId })
