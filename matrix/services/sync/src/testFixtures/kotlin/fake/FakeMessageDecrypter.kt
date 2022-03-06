package fake

import app.dapk.st.matrix.common.EncryptedMessageContent
import app.dapk.st.matrix.sync.internal.room.MessageDecrypter
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeMessageDecrypter : MessageDecrypter by mockk() {

    fun givenDecrypt(content: EncryptedMessageContent) = coEvery { decrypt(content) }.delegateReturn()
}