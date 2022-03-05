package internalfake

import app.dapk.st.matrix.common.DeviceCredentials
import app.dapk.st.matrix.crypto.internal.EncryptMessageWithMegolmUseCase
import app.dapk.st.matrix.crypto.internal.MessageToEncrypt
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeEncryptMessageWithMegolmUseCase : EncryptMessageWithMegolmUseCase by mockk() {
    fun givenEncrypt(credentials: DeviceCredentials, message: MessageToEncrypt) = coEvery {
        this@FakeEncryptMessageWithMegolmUseCase.invoke(credentials, message)
    }.delegateReturn()
}