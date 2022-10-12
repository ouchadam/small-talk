package app.dapk.st.engine

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.message.MessageService
import fixture.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ROOM_MESSAGE_EVENT = aRoomMessageEvent(eventId = anEventId("1"))
private val A_ROOM_IMAGE_MESSAGE_EVENT = aRoomMessageEvent(eventId = anEventId("2"))
private val A_LOCAL_ECHO_EVENT_ID = anEventId("2")
private const val A_LOCAL_ECHO_BODY = "body"
private val A_ROOM_MEMBER = aRoomMember()
private val ANOTHER_ROOM_MESSAGE_EVENT = A_ROOM_MESSAGE_EVENT.copy(eventId = anEventId("a second room event"))

class MergeWithLocalEchosUseCaseTest {

    private val fakeLocalEchoMapper = fake.FakeLocalEventMapper()
    private val mergeWithLocalEchosUseCase = MergeWithLocalEchosUseCaseImpl(fakeLocalEchoMapper.instance)

    @Test
    fun `given no local echos, when merging text message, then returns original state`() {
        val roomState = aRoomState(events = listOf(A_ROOM_MESSAGE_EVENT)).engine()

        val result = mergeWithLocalEchosUseCase.invoke(roomState, A_ROOM_MEMBER, emptyList())

        result shouldBeEqualTo roomState
    }

    @Test
    fun `given no local echos, when merging events, then returns original ordered by timestamp descending`() {
        val roomState = aRoomState(events = listOf(A_ROOM_IMAGE_MESSAGE_EVENT.copy(utcTimestamp = 1500), A_ROOM_MESSAGE_EVENT.copy(utcTimestamp = 1000))).engine()

        val result = mergeWithLocalEchosUseCase.invoke(roomState, A_ROOM_MEMBER, emptyList())

        result shouldBeEqualTo roomState.copy(events = roomState.events.sortedByDescending { it.utcTimestamp })
    }

    @Test
    fun `given local echo with sending state, when merging then maps to room event with local echo state`() {
        val second = createLocalEcho(A_LOCAL_ECHO_EVENT_ID, A_LOCAL_ECHO_BODY, state = MessageService.LocalEcho.State.Sending)
        fakeLocalEchoMapper.givenMapping(second, A_ROOM_MEMBER).returns(ANOTHER_ROOM_MESSAGE_EVENT.engine())
        val roomState = aRoomState(events = listOf(A_ROOM_MESSAGE_EVENT)).engine()

        val result = mergeWithLocalEchosUseCase.invoke(roomState, A_ROOM_MEMBER, listOf(second))

        result shouldBeEqualTo roomState.copy(
            events = listOf(
                A_ROOM_MESSAGE_EVENT.engine(),
                ANOTHER_ROOM_MESSAGE_EVENT.engine(),
            )
        )
    }

    private fun createLocalEcho(eventId: EventId, body: String, state: MessageService.LocalEcho.State) = aLocalEcho(
        eventId,
        aTextMessage(aTextContent(body)),
        state,
    )
}