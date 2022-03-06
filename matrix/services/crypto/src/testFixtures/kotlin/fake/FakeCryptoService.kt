package fake

import app.dapk.st.matrix.crypto.CryptoService
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn
import java.io.InputStream

class FakeCryptoService : CryptoService by mockk() {
    fun givenImportKeys(inputStream: InputStream, passphrase: String) = coEvery { inputStream.importRoomKeys(passphrase) }.delegateReturn()
}
