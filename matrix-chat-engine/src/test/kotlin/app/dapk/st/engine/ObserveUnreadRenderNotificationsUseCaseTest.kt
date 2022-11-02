package app.dapk.st.engine

import app.dapk.st.matrix.common.RichText
import fake.FakeRoomStore
import fixture.NotificationDiffFixtures.aNotificationDiff
import fixture.aMatrixRoomMessageEvent
import fixture.aMatrixRoomOverview
import fixture.aRoomId
import fixture.anEventId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import app.dapk.st.matrix.sync.RoomEvent as MatrixRoomEvent
import app.dapk.st.matrix.sync.RoomOverview as MatrixRoomOverview

private val NO_UNREADS = emptyMap<MatrixRoomOverview, List<MatrixRoomEvent>>()
private val A_MESSAGE = aMatrixRoomMessageEvent(eventId = anEventId("1"), content = RichText.of("hello"), utcTimestamp = 1000)
private val A_MESSAGE_2 = aMatrixRoomMessageEvent(eventId = anEventId("2"), content = RichText.of("world"), utcTimestamp = 2000)
private val A_ROOM_OVERVIEW = aMatrixRoomOverview(roomId = aRoomId("1"))
private val A_ROOM_OVERVIEW_2 = aMatrixRoomOverview(roomId = aRoomId("2"))

private fun MatrixRoomOverview.withUnreads(vararg events: MatrixRoomEvent) = mapOf(this to events.toList())
private fun MatrixRoomOverview.toDiff(vararg events: MatrixRoomEvent) = mapOf(this.roomId to events.map { it.eventId })

class ObserveUnreadRenderNotificationsUseCaseTest {

    private val fakeRoomStore = FakeRoomStore()

    private val useCase = ObserveUnreadNotificationsUseCaseImpl(fakeRoomStore)

    @Test
    fun `given no initial unreads, when receiving new message, then emits message`() = runTest {
        givenNoInitialUnreads(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE).engine() to aNotificationDiff(
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
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE).engine() to aNotificationDiff(
                changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE),
                newRooms = setOf(A_ROOM_OVERVIEW.roomId)
            ),
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2).engine() to aNotificationDiff(changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE_2))
        )
    }

    @Test
    fun `given initial unreads, when receiving new message, then emits all messages`() = runTest {
        fakeRoomStore.givenNotMutedUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE_2).engine() to aNotificationDiff(changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE_2))
        )
    }

    @Test
    fun `given initial unreads, when reading a message, then emits nothing`() = runTest {
        fakeRoomStore.givenNotMutedUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE) + A_ROOM_OVERVIEW_2.withUnreads(A_MESSAGE_2), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo emptyList()
    }

    @Test
    fun `given new and then historical message, when reading a message, then only emits the latest`() = runTest {
        fakeRoomStore.givenNotMutedUnreadEvents(
            flowOf(
                NO_UNREADS,
                A_ROOM_OVERVIEW.withUnreads(A_MESSAGE),
                A_ROOM_OVERVIEW.withUnreads(A_MESSAGE, A_MESSAGE.copy(eventId = anEventId("old"), utcTimestamp = -1))
            )
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo listOf(
            A_ROOM_OVERVIEW.withUnreads(A_MESSAGE).engine() to aNotificationDiff(
                changedOrNew = A_ROOM_OVERVIEW.toDiff(A_MESSAGE),
                newRooms = setOf(A_ROOM_OVERVIEW.roomId)
            ),
        )
    }

    @Test
    fun `given initial unreads, when reading a duplicate unread, then emits nothing`() = runTest {
        fakeRoomStore.givenNotMutedUnreadEvents(
            flowOf(A_ROOM_OVERVIEW.withUnreads(A_MESSAGE), A_ROOM_OVERVIEW.withUnreads(A_MESSAGE))
        )

        val result = useCase.invoke().toList()

        result shouldBeEqualTo emptyList()
    }

    private fun givenNoInitialUnreads(vararg unreads: Map<MatrixRoomOverview, List<MatrixRoomEvent>>) =
        fakeRoomStore.givenNotMutedUnreadEvents(flowOf(NO_UNREADS, *unreads))
}

private fun Map<MatrixRoomOverview, List<MatrixRoomEvent>>.engine() = this
    .mapKeys { it.key.engine() }
    .mapValues { it.value.map { it.engine() } }
