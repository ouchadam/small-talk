import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.room.roomService
import app.dapk.st.matrix.sync.syncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import test.MatrixTestScope
import test.TestMatrix
import test.flowTest
import test.restoreLoginAndInitialSync
import java.util.*

private const val TEST_SERVER_URL_REDIRECT = "http://localhost:8080/"
private const val HTTPS_TEST_SERVER_URL = "https://localhost:8480/"

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SmokeTest {

    @Test
    @Order(1)
    fun `can register accounts`() = runTest {
        SharedState._alice = createAndRegisterAccount()
        SharedState._bob = createAndRegisterAccount()
    }

    @Test
    @Order(2)
    fun `can login`() = runTest {
        login(SharedState.alice)
        login(SharedState.bob)
    }

    @Test
    @Order(3)
    fun `can create and join rooms`() = flowTest {
        val alice = TestMatrix(SharedState.alice, includeLogging = true).also { it.loginWithInitialSync() }

        val roomId = alice.client.roomService().createDm(SharedState.bob.roomMember.id, encrypted = true)
        alice.client.syncService().startSyncing().collectAsync {
            alice.expectRoom(roomId)
        }

        val bob = TestMatrix(SharedState.bob, includeLogging = true).also { it.loginWithInitialSync() }
        bob.client.syncService().startSyncing().collectAsync {
            bob.expectInvite(roomId)
            bob.client.roomService().joinRoom(roomId)
            bob.expectRoom(roomId)
        }

        SharedState._sharedRoom = roomId
    }

    @Test
    @Order(4)
    fun `can send and receive encrypted messages`() = testAfterInitialSync { alice, bob ->
        val message = "from alice to bob : ${System.currentTimeMillis()}".from(SharedState.alice.roomMember)
        alice.sendEncryptedMessage(SharedState.sharedRoom, message.content)
        bob.expectMessage(SharedState.sharedRoom, message)

        val message2 = "from bob to alice : ${System.currentTimeMillis()}".from(SharedState.bob.roomMember)
        bob.sendEncryptedMessage(SharedState.sharedRoom, message2.content)
        alice.expectMessage(SharedState.sharedRoom, message2)

        val aliceSecondDevice = TestMatrix(SharedState.alice).also { it.newlogin() }
        aliceSecondDevice.client.syncService().startSyncing().collectAsync {
            val message3 = "from alice to bob and alice's second device : ${System.currentTimeMillis()}".from(SharedState.alice.roomMember)
            alice.sendEncryptedMessage(SharedState.sharedRoom, message3.content)
            aliceSecondDevice.expectMessage(SharedState.sharedRoom, message3)
            bob.expectMessage(SharedState.sharedRoom, message3)

            val message4 = "from alice's second device to bob and alice's first device : ${System.currentTimeMillis()}".from(SharedState.alice.roomMember)
            aliceSecondDevice.sendEncryptedMessage(SharedState.sharedRoom, message4.content)
            alice.expectMessage(SharedState.sharedRoom, message4)
            bob.expectMessage(SharedState.sharedRoom, message4)
        }
    }

    @Test
    @Order(5)
    fun `can request and verify devices`() = testAfterInitialSync { alice, bob ->
        alice.client.cryptoService().verificationAction(Verification.Action.Request(bob.userId(), bob.deviceId()))
        alice.client.cryptoService().verificationState().automaticVerification(alice).expectAsync { it == Verification.State.Done }
        bob.client.cryptoService().verificationState().automaticVerification(bob).expectAsync { it == Verification.State.Done }

        waitForExpects()
    }

    @Test
    fun `can import E2E room keys file`() = runTest {
        val ignoredUser = TestUser("ignored", RoomMember(UserId("ignored"), null, null), "ignored")
        val cryptoService = TestMatrix(ignoredUser, includeLogging = true).client.cryptoService()
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("element-keys.txt")!!

        val result = with(cryptoService) {
            stream.importRoomKeys(password = "aaaaaa")
        }

        result shouldBeEqualTo listOf(RoomId(value="!qOSENTtFUuCEKJSVzl:matrix.org"))
    }
}

private suspend fun createAndRegisterAccount(): TestUser {
    val aUserName = "${UUID.randomUUID()}"
    val userId = UserId("@$aUserName:localhost:8480")
    val aUser = TestUser("aaaa11111zzzz", RoomMember(userId, aUserName, null), HTTPS_TEST_SERVER_URL)

    val result = TestMatrix(aUser, includeLogging = true, includeHttpLogging = true)
        .client
        .authService()
        .register(aUserName, aUser.password, homeServer = HTTPS_TEST_SERVER_URL)

    result.accessToken shouldNotBeEqualTo null
    result.homeServer shouldBeEqualTo HomeServerUrl(TEST_SERVER_URL_REDIRECT)
    result.userId shouldBeEqualTo userId
    return aUser
}

private suspend fun login(user: TestUser) {
    val testMatrix = TestMatrix(user, includeLogging = true)
    val result = testMatrix
        .client
        .authService()
        .login(userName = user.roomMember.id.value, password = user.password)

    result.accessToken shouldNotBeEqualTo null
    result.homeServer shouldBeEqualTo HomeServerUrl(TEST_SERVER_URL_REDIRECT)
    result.userId shouldBeEqualTo user.roomMember.id

    testMatrix.saveLogin(result)
}

object SharedState {
    val alice: TestUser
        get() = _alice!!
    var _alice: TestUser? = null

    val bob: TestUser
        get() = _bob!!
    var _bob: TestUser? = null

    val sharedRoom: RoomId
        get() = _sharedRoom!!
    var _sharedRoom: RoomId? = null
}

data class TestUser(val password: String, val roomMember: RoomMember, val homeServer: String)
data class TestMessage(val content: String, val author: RoomMember)

fun String.from(roomMember: RoomMember) = TestMessage(this, roomMember)

fun testAfterInitialSync(block: suspend MatrixTestScope.(TestMatrix, TestMatrix) -> Unit) {
    restoreLoginAndInitialSync(TestMatrix(SharedState.alice, includeLogging = false), TestMatrix(SharedState.bob, includeLogging = false), block)
}

private fun Flow<Verification.State>.automaticVerification(testMatrix: TestMatrix) = this.onEach {
    when (it) {
        is Verification.State.WaitingForMatchConfirmation -> testMatrix.client.cryptoService().verificationAction(Verification.Action.AcknowledgeMatch)
    }
}