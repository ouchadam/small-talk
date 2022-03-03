package internalfake

import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.internal.RegisterOlmSessionUseCase
import app.dapk.st.matrix.device.internal.DeviceKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import test.delegateReturn

internal class FakeRegisterOlmSessionUseCase : RegisterOlmSessionUseCase by mockk() {

    fun givenRegistersSessions(devices: List<DeviceKeys>, account: Olm.AccountCryptoSession) = coEvery {
        this@FakeRegisterOlmSessionUseCase.invoke(devices, account)
    }.delegateReturn()

    fun verifyRegistersKeys(devices: List<DeviceKeys>, account: Olm.AccountCryptoSession) {
        coVerify { this@FakeRegisterOlmSessionUseCase.invoke(devices, account) }
    }

    fun verifyNoInteractions() {
        coVerify(exactly = 0) { this@FakeRegisterOlmSessionUseCase.invoke(any(), any()) }
    }
}