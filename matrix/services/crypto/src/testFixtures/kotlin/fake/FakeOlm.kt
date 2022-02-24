package fake

import app.dapk.st.matrix.common.CipherText
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import test.Returns
import test.delegateReturn

class FakeOlm : Olm by mockk() {

    fun givenEncrypts(roomCryptoSession: Olm.RoomCryptoSession, roomId: RoomId, messageJson: JsonString, result: CipherText) {
        coEvery { roomCryptoSession.encrypt(roomId, messageJson) } returns result
    }

    fun givenEncrypts(olmSession: Olm.DeviceCryptoSession, messageJson: JsonString) = coEvery { olmSession.encrypt(messageJson) }.delegateReturn()

    fun givenCreatesAccount(credentials: UserCredentials): Returns<Olm.AccountCryptoSession> {
        val slot = slot<suspend (Olm.AccountCryptoSession) -> Unit>()
        val mockKStubScope = coEvery { ensureAccountCrypto(credentials, capture(slot)) }
        return Returns { value ->
            mockKStubScope coAnswers {
                slot.captured.invoke(value)
                value
            }
        }
    }

    fun givenAccount(credentials: UserCredentials): Returns<Olm.AccountCryptoSession> {
        return coEvery { ensureAccountCrypto(credentials, any()) }.delegateReturn()
    }

    fun givenRoomCrypto(roomId: RoomId, account: Olm.AccountCryptoSession) = coEvery { ensureRoomCrypto(roomId, account) }.delegateReturn()

    fun givenMissingOlmSessions(newDevices: List<DeviceKeys>): Returns<List<Olm.DeviceCryptoSession>> {
        val slot = slot<suspend (List<DeviceKeys>) -> List<Olm.DeviceCryptoSession>>()
        val mockKStubScope = coEvery { olmSessions(newDevices, capture(slot)) }
        return Returns { value ->
            mockKStubScope coAnswers {
                slot.captured.invoke(newDevices).also {
                    value shouldBeEqualTo it
                }
            }
        }
    }

    fun givenGeneratesOneTimeKeys(
        accountCryptoSession: Olm.AccountCryptoSession,
        countToCreate: Int,
        credentials: UserCredentials
    ): Returns<DeviceService.OneTimeKeys> {
        val slot = slot<suspend (DeviceService.OneTimeKeys) -> Unit>()
        val mockKStubScope = coEvery { with(accountCryptoSession) { generateOneTimeKeys(countToCreate, credentials, capture(slot)) } }
        return Returns { value ->
            mockKStubScope coAnswers {
                slot.captured.invoke(value)
            }
        }
    }

    fun givenDeviceCrypto(input: Olm.OlmSessionInput, account: Olm.AccountCryptoSession) = coEvery { ensureDeviceCrypto(input, account) }.delegateReturn()
}