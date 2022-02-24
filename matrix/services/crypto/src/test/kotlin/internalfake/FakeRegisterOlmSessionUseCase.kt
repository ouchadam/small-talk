package internalfake

import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.internal.RegisterOlmSessionUseCase
import app.dapk.st.matrix.device.internal.DeviceKeys
import io.mockk.coEvery
import io.mockk.mockk
import test.Returns
import test.delegateReturn

internal class FakeRegisterOlmSessionUseCase : RegisterOlmSessionUseCase by mockk() {
    fun givenRegistersSessions(devices: List<DeviceKeys>, account: Olm.AccountCryptoSession): Returns<List<Olm.DeviceCryptoSession>> {
        return coEvery { this@FakeRegisterOlmSessionUseCase.invoke(devices, account) }.delegateReturn()
    }
}