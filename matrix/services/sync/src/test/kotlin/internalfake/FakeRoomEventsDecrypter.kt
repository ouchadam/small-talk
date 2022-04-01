package internalfake

import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.internal.room.RoomEventsDecrypter
import io.mockk.coEvery
import io.mockk.mockk

internal class FakeRoomEventsDecrypter {
    val instance = mockk<RoomEventsDecrypter>()

    fun givenDecrypts(userCredentials: UserCredentials, previousEvents: List<RoomEvent>, result: List<RoomEvent> = previousEvents) {
        coEvery { instance.decryptRoomEvents(userCredentials, previousEvents) } returns result
    }
}