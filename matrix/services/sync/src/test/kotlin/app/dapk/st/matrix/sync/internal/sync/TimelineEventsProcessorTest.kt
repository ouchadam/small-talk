package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.request.ApiSyncRoom
import fixture.*
import internalfake.FakeEventLookup
import internalfake.FakeRoomEventCreator
import internalfake.FakeRoomEventsDecrypter
import internalfake.FakeSyncEventDecrypter
import internalfixture.*
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ROOM_ID = aRoomId()
private val ANY_LOOKUP_RESULT = LookupResult(anApiTimelineTextEvent(), roomEvent = null)
private val AN_ENCRYPTED_TIMELINE_EVENT = anEncryptedApiTimelineEvent()
private val A_TEXT_TIMELINE_EVENT = anApiTimelineTextEvent()
private val A_MESSAGE_ROOM_EVENT = aMatrixRoomMessageEvent(anEventId("a-message"))
private val AN_ENCRYPTED_ROOM_EVENT = anEncryptedRoomMessageEvent(anEventId("encrypted-message"))
private val A_LOOKUP_EVENT_ID = anEventId("lookup-id")
private val A_USER_CREDENTIALS = aUserCredentials()

class TimelineEventsProcessorTest {

    private val fakeRoomEventsDecrypter = FakeRoomEventsDecrypter()
    private val fakeSyncEventDecrypter = FakeSyncEventDecrypter()
    private val fakeRoomEventCreator = FakeRoomEventCreator()
    private val fakeEventLookup = FakeEventLookup()

    private val timelineEventsProcessor = TimelineEventsProcessor(
        fakeRoomEventCreator.instance,
        fakeRoomEventsDecrypter.instance,
        fakeSyncEventDecrypter.instance,
        fakeEventLookup.instance,
    )

    @Test
    fun `given a room with no events then returns empty`() = runTest {
        val previousEvents = emptyList<RoomEvent>()
        val roomToProcess = aRoomToProcess()
        fakeRoomEventsDecrypter.givenDecrypts(A_USER_CREDENTIALS, previousEvents)
        fakeSyncEventDecrypter.givenDecrypts(roomToProcess.apiSyncRoom.timeline.apiTimelineEvents)

        val result = timelineEventsProcessor.process(roomToProcess, previousEvents)

        result shouldBeEqualTo (emptyList<RoomEvent>() to emptyList())
    }

    @Test
    fun `given encrypted and text timeline events when processing then maps to room events`() = runTest {
        val previousEvents = listOf(aMatrixRoomMessageEvent(eventId = anEventId("previous-event")))
        val newTimelineEvents = listOf(AN_ENCRYPTED_TIMELINE_EVENT, A_TEXT_TIMELINE_EVENT)
        val roomToProcess = aRoomToProcess(apiSyncRoom = anApiSyncRoom(anApiSyncRoomTimeline(newTimelineEvents)))
        fakeRoomEventsDecrypter.givenDecrypts(A_USER_CREDENTIALS, previousEvents)
        fakeSyncEventDecrypter.givenDecrypts(newTimelineEvents)
        fakeEventLookup.givenLookup(A_LOOKUP_EVENT_ID, DecryptedTimeline(newTimelineEvents), DecryptedRoomEvents(previousEvents), ANY_LOOKUP_RESULT)
        fakeRoomEventCreator.givenCreates(A_ROOM_ID, AN_ENCRYPTED_TIMELINE_EVENT, AN_ENCRYPTED_ROOM_EVENT)
        fakeRoomEventCreator.givenCreatesUsingLookup(
            A_USER_CREDENTIALS,
            A_ROOM_ID,
            A_LOOKUP_EVENT_ID,
            A_TEXT_TIMELINE_EVENT,
            A_MESSAGE_ROOM_EVENT,
            ANY_LOOKUP_RESULT
        )

        val result = timelineEventsProcessor.process(roomToProcess, previousEvents)

        val expectedNewRoomEvents = listOf(AN_ENCRYPTED_ROOM_EVENT, A_MESSAGE_ROOM_EVENT)
        result shouldBeEqualTo (expectedNewRoomEvents to expectedNewRoomEvents + previousEvents)
    }

    @Test
    fun `given unhandled timeline events when processing then ignores events`() = runTest {
        val previousEvents = emptyList<RoomEvent>()
        val newTimelineEvents = listOf(
            anEncryptionApiTimelineEvent(),
            aRoomAvatarApiTimelineEvent(),
            aRoomCreateApiTimelineEvent(),
            aRoomMemberApiTimelineEvent(),
            aRoomNameApiTimelineEvent(),
            aRoomTopicApiTimelineEvent(),
            anIgnoredApiTimelineEvent()
        )
        val roomToProcess = aRoomToProcess(apiSyncRoom = anApiSyncRoom(anApiSyncRoomTimeline(newTimelineEvents)))
        fakeRoomEventsDecrypter.givenDecrypts(A_USER_CREDENTIALS, previousEvents)
        fakeSyncEventDecrypter.givenDecrypts(newTimelineEvents)

        val result = timelineEventsProcessor.process(roomToProcess, previousEvents)

        result shouldBeEqualTo (emptyList<RoomEvent>() to emptyList())
    }
}

internal fun aRoomToProcess(
    roomId: RoomId = aRoomId(),
    apiSyncRoom: ApiSyncRoom = anApiSyncRoom(),
    directMessage: UserId? = null,
    userCredentials: UserCredentials = aUserCredentials(),
) = RoomToProcess(roomId, apiSyncRoom, directMessage, userCredentials, heroes = null)
