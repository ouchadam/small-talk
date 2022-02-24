package internalfake

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.internal.FetchMegolmSessionUseCase
import io.mockk.coEvery
import io.mockk.mockk

internal class FakeFetchMegolmSessionUseCase : FetchMegolmSessionUseCase by mockk() {
    fun givenSessionForRoom(roomId: RoomId, roomCryptoSession: Olm.RoomCryptoSession) {
        coEvery { this@FakeFetchMegolmSessionUseCase.invoke(roomId) } returns roomCryptoSession
    }
}