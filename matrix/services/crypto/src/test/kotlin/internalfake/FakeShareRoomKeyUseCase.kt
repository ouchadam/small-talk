package internalfake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.internal.ShareRoomKeyUseCase
import io.mockk.coJustRun
import io.mockk.mockk

internal class FakeShareRoomKeyUseCase : ShareRoomKeyUseCase {

    private val instance = mockk<ShareRoomKeyUseCase>()

    override suspend fun invoke(room: Olm.RoomCryptoSession, p2: List<Olm.DeviceCryptoSession>, p3: RoomId) {
        instance.invoke(room, p2, p3)
    }

    fun expect(roomCryptoSession: Olm.RoomCryptoSession, olmSessions: List<Olm.DeviceCryptoSession>, roomId: RoomId) {
        coJustRun {
            instance.invoke(roomCryptoSession, olmSessions, roomId)
        }
    }

}