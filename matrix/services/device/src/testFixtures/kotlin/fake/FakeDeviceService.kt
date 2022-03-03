package fake

import app.dapk.st.matrix.common.SessionId
import app.dapk.st.matrix.common.SyncToken
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import test.Returns
import test.delegateReturn

class FakeDeviceService : DeviceService by mockk() {
    fun givenNewDevices(accountKeys: DeviceKeys, usersInRoom: List<UserId>, roomCryptoSessionId: SessionId): Returns<List<DeviceKeys>> {
        return coEvery { checkForNewDevices(accountKeys, usersInRoom, roomCryptoSessionId) }.delegateReturn()
    }

    fun verifyDidntUploadOneTimeKeys() {
        coVerify(exactly = 0) { uploadOneTimeKeys(DeviceService.OneTimeKeys(any())) }
    }

    fun givenClaimsKeys(claims: List<DeviceService.KeyClaim>) = coEvery { claimKeys(claims) }.delegateReturn()

    fun givenFetchesDevices(userIds: List<UserId>, syncToken: SyncToken?) = coEvery { fetchDevices(userIds, syncToken) }.delegateReturn()
}