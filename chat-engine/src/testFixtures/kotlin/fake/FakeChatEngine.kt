package fake

import app.dapk.st.engine.ChatEngine
import app.dapk.st.matrix.common.RoomId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import test.delegateReturn
import java.io.InputStream

class FakeChatEngine : ChatEngine by mockk() {

    fun givenMessages(roomId: RoomId, disableReadReceipts: Boolean) = every { messages(roomId, disableReadReceipts) }.delegateReturn()

    fun givenDirectory() = every { directory() }.delegateReturn()

    fun givenImportKeys(inputStream: InputStream, passphrase: String) = coEvery { inputStream.importRoomKeys(passphrase) }.delegateReturn()

}