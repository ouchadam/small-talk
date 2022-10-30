package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import fake.FakeRoomStore
import fixture.aMatrixRoomMessageEvent
import fixture.anEventId
import internalfixture.aTimelineTextEventContent
import internalfixture.anApiTimelineTextEvent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val AN_EVENT_ID = anEventId()
private val A_TIMELINE_EVENT = anApiTimelineTextEvent(AN_EVENT_ID, content = aTimelineTextEventContent(body = "timeline event"))
private val A_ROOM_EVENT = aMatrixRoomMessageEvent(AN_EVENT_ID, content = RichText.of("previous room event"))
private val A_PERSISTED_EVENT = aMatrixRoomMessageEvent(AN_EVENT_ID, content = RichText.of("persisted event"))

class EventLookupUseCaseTest {

    private val fakeRoomStore = FakeRoomStore()

    private val eventLookupUseCase = EventLookupUseCase(fakeRoomStore)

    @Test
    fun `given all lookup sources fail then returns null results`() = runTest {
        fakeRoomStore.givenEvent(AN_EVENT_ID, result = null)

        val result = eventLookupUseCase.lookup(
            AN_EVENT_ID,
            DecryptedTimeline(emptyList()),
            DecryptedRoomEvents(emptyList())
        )

        result shouldBeEqualTo LookupResult(null, null)
    }

    @Test
    fun `when looking up event then prioritises timeline result first`() = runTest {
        fakeRoomStore.givenEvent(AN_EVENT_ID, result = A_PERSISTED_EVENT)

        val result = eventLookupUseCase.lookup(
            AN_EVENT_ID,
            DecryptedTimeline(listOf(A_TIMELINE_EVENT)),
            DecryptedRoomEvents(listOf(A_ROOM_EVENT))
        )

        result shouldBeEqualTo LookupResult(apiTimelineEvent = A_TIMELINE_EVENT, null)
    }

    @Test
    fun `given no timeline event when looking up event then returns previous room result`() = runTest {
        fakeRoomStore.givenEvent(AN_EVENT_ID, result = A_PERSISTED_EVENT)

        val result = eventLookupUseCase.lookup(
            AN_EVENT_ID,
            DecryptedTimeline(emptyList()),
            DecryptedRoomEvents(listOf(A_ROOM_EVENT))
        )

        result shouldBeEqualTo LookupResult(apiTimelineEvent = null, roomEvent = A_ROOM_EVENT)
    }

    @Test
    fun `given no timeline or previous room event when looking up event then returns persisted room result`() = runTest {
        fakeRoomStore.givenEvent(AN_EVENT_ID, result = A_PERSISTED_EVENT)

        val result = eventLookupUseCase.lookup(
            AN_EVENT_ID,
            DecryptedTimeline(emptyList()),
            DecryptedRoomEvents(emptyList())
        )

        result shouldBeEqualTo LookupResult(apiTimelineEvent = null, roomEvent = A_PERSISTED_EVENT)
    }
}