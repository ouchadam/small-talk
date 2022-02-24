package internalfake

import app.dapk.st.matrix.sync.internal.request.ApiTimelineEvent
import app.dapk.st.matrix.sync.internal.room.SyncEventDecrypter
import io.mockk.coEvery
import io.mockk.mockk

internal class FakeSyncEventDecrypter {
    val instance = mockk<SyncEventDecrypter>()

    fun givenDecrypts(events: List<ApiTimelineEvent>, result: List<ApiTimelineEvent> = events) {
        coEvery { instance.decryptTimelineEvents(events) } returns result
    }
}