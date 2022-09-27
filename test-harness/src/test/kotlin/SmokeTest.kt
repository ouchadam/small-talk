import app.dapk.st.matrix.auth.AuthService
import app.dapk.st.matrix.auth.authService
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.crypto.ImportResult
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.crypto.cryptoService
import app.dapk.st.matrix.room.roomService
import app.dapk.st.matrix.sync.syncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import test.*
import java.nio.file.Paths
import java.util.*

private const val HTTPS_TEST_SERVER_URL = "https://localhost:8080/"

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SmokeTest {

    @Test
    @Order(1)
    fun `can register accounts`() = runTest {
        SharedState._alice = createAndRegisterAccount("alice")
        SharedState._bob = createAndRegisterAccount("bob")
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
    fun `can send and receive clear text messages`() = testTextMessaging(isEncrypted = false)

    @Test
    @Order(5)
    fun `can send and receive encrypted text messages`() = testTextMessaging(isEncrypted = true)

    @Test
    @Order(6)
    fun `can send and receive clear image messages`() = testAfterInitialSync { alice, bob ->
        val testImage = loadResourceFile("test-image.png")
        alice.sendImageMessage(SharedState.sharedRoom, testImage, isEncrypted = false)
        bob.expectImageMessage(SharedState.sharedRoom, testImage, SharedState.alice.roomMember)
    }

    @Test
    @Order(7)
    fun `can send and receive encrypted image messages`() = testAfterInitialSync { alice, bob ->
        val testImage = loadResourceFile("test-image2.png")
        alice.sendImageMessage(SharedState.sharedRoom, testImage, isEncrypted = true)
        bob.expectImageMessage(SharedState.sharedRoom, testImage, SharedState.alice.roomMember)
    }

    @Test
    @Order(8)
    fun `can request and verify devices`() = testAfterInitialSync { alice, bob ->
        alice.client.cryptoService().verificationAction(Verification.Action.Request(bob.userId(), bob.deviceId()))
        alice.client.cryptoService().verificationState().automaticVerification(alice).expectAsync { it == Verification.State.Done }
        bob.client.cryptoService().verificationState().automaticVerification(bob).expectAsync { it == Verification.State.Done }

        waitForExpects()
    }

    @Test
    fun `can import E2E room keys file`() = runTest {
        val ignoredUser = TestUser("ignored", RoomMember(UserId("ignored"), null, null), "ignored", "ignored")
        val cryptoService = TestMatrix(ignoredUser, includeLogging = true).client.cryptoService()
        val stream = loadResourceStream("element-keys.txt")

        val result = with(cryptoService) {
            stream.importRoomKeys(password = "aaaaaa").first { it is ImportResult.Success }
        }

        result shouldBeEqualTo ImportResult.Success(
            roomIds = setOf(RoomId(value = "!qOSENTtFUuCEKJSVzl:matrix.org")),
            totalImportedKeysCount = 28,
        )
    }

    private fun testTextMessaging(isEncrypted: Boolean) = testAfterInitialSync { alice, bob ->
        val message = "from alice to bob".from(SharedState.alice.roomMember)
        alice.sendTextMessage(SharedState.sharedRoom, message.content, isEncrypted)
        bob.expectTextMessage(SharedState.sharedRoom, message)

        val message2 = "from bob to alice".from(SharedState.bob.roomMember)
        bob.sendTextMessage(SharedState.sharedRoom, message2.content, isEncrypted)
        alice.expectTextMessage(SharedState.sharedRoom, message2)

        // Needs investigation
        val aliceSecondDevice = testMatrix(SharedState.alice, isTemp = true, withLogging = true).also { it.newlogin() }
        aliceSecondDevice.client.syncService().startSyncing().collectAsync {
            aliceSecondDevice.client.proxyDeviceService().waitForOneTimeKeysToBeUploaded()

            val message3 = "from alice to bob and alice's second device".from(SharedState.alice.roomMember)
            alice.sendTextMessage(SharedState.sharedRoom, message3.content, isEncrypted)
            aliceSecondDevice.expectTextMessage(SharedState.sharedRoom, message3)
            bob.expectTextMessage(SharedState.sharedRoom, message3)

            val message4 = "from alice's second device to bob and alice's first device".from(SharedState.alice.roomMember)
            aliceSecondDevice.sendTextMessage(SharedState.sharedRoom, message4.content, isEncrypted)
            alice.expectTextMessage(SharedState.sharedRoom, message4)
            bob.expectTextMessage(SharedState.sharedRoom, message4)
        }
    }
}

private suspend fun createAndRegisterAccount(testUsername: String): TestUser {
    val aUserName = "${UUID.randomUUID()}"
    val userId = UserId("@$aUserName:localhost:8080")
    val aUser = TestUser("aaaa11111zzzz", RoomMember(userId, aUserName, null), HTTPS_TEST_SERVER_URL, testUsername)

    val result = TestMatrix(aUser, includeLogging = true, includeHttpLogging = true)
        .client
        .authService()
        .register(aUserName, aUser.password, homeServer = HTTPS_TEST_SERVER_URL)

    result.accessToken shouldNotBeEqualTo null
    result.homeServer shouldBeEqualTo HomeServerUrl(HTTPS_TEST_SERVER_URL)
    result.userId shouldBeEqualTo userId
    return aUser
}

private suspend fun login(user: TestUser) {
    val testMatrix = TestMatrix(user, includeLogging = true)
    val result = testMatrix
        .client
        .authService()
        .login(AuthService.LoginRequest(userName = user.roomMember.id.value, password = user.password, serverUrl = null))

    result shouldBeInstanceOf AuthService.LoginResult.Success::class.java
    (result as AuthService.LoginResult.Success).userCredentials.let { credentials ->
        credentials.accessToken shouldNotBeEqualTo null
        credentials.homeServer shouldBeEqualTo HomeServerUrl(HTTPS_TEST_SERVER_URL)
        credentials.userId shouldBeEqualTo user.roomMember.id

        testMatrix.saveLogin(credentials)
    }
}

object SharedState {

    val alice: TestUser
        get() = _alice!!
    var _alice: TestUser? = null
        set(value) {
            field = value!!
            TestUsers.users.add(value)
        }

    val bob: TestUser
        get() = _bob!!
    var _bob: TestUser? = null
        set(value) {
            field = value!!
            TestUsers.users.add(value)
        }

    val sharedRoom: RoomId
        get() = _sharedRoom!!
    var _sharedRoom: RoomId? = null
}

data class TestUser(val password: String, val roomMember: RoomMember, val homeServer: String, val testName: String)
data class TestMessage(val content: String, val author: RoomMember)

fun String.from(roomMember: RoomMember) = TestMessage("$this - ${UUID.randomUUID()}", roomMember)

fun testAfterInitialSync(block: suspend MatrixTestScope.(TestMatrix, TestMatrix) -> Unit) {
    restoreLoginAndInitialSync(TestMatrix(SharedState.alice, includeLogging = true), TestMatrix(SharedState.bob, includeLogging = false), block)
}

private fun Flow<Verification.State>.automaticVerification(testMatrix: TestMatrix) = this.onEach {
    when (it) {
        is Verification.State.WaitingForMatchConfirmation -> testMatrix.client.cryptoService().verificationAction(Verification.Action.AcknowledgeMatch)
        else -> {
            // do nothing
        }
    }
}

private fun loadResourceStream(name: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(name)!!
private fun loadResourceFile(name: String) = Paths.get(Thread.currentThread().contextClassLoader.getResource(name)!!.toURI()).toFile()
