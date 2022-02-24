package internalfake

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.sync.internal.sync.DecryptedRoomEvents
import app.dapk.st.matrix.sync.internal.sync.DecryptedTimeline
import app.dapk.st.matrix.sync.internal.sync.EventLookupUseCase
import app.dapk.st.matrix.sync.internal.sync.LookupResult
import io.mockk.coEvery
import io.mockk.mockk

internal class FakeEventLookup {
    val instance = mockk<EventLookupUseCase>()

    fun givenLookup(eventId: EventId, timeline: DecryptedTimeline, previousEvents: DecryptedRoomEvents, result: LookupResult) {
        coEvery { instance.lookup(eventId, timeline, previousEvents) } returns result
    }
}