package app.dapk.st.login.state

import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import app.dapk.st.push.PushTokenRegistrar
import fake.FakeChatEngine
import fake.FakeErrorTracker
import fixture.aUserId
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.expect

private val A_LOGIN_ERROR = LoginResult.Error(RuntimeException())
private val A_LOGIN_SUCCESS = LoginResult.Success(aUserCredentials())

private val A_LOGIN_REQUEST = LoginRequest(
    userName = "a-username",
    password = "a-password",
    serverUrl = "a-server-url",
)

class LoginUseCaseTest {

    private val fakeChatEngine = FakeChatEngine()
    private val fakePushTokenRegistrar = FakePushTokenRegistrar()
    private val fakeErrorTracker = FakeErrorTracker()

    private val useCase = LoginUseCase(
        fakeChatEngine,
        fakePushTokenRegistrar,
        fakeErrorTracker,
    )

    @Test
    fun `when logging in succeeds, then registers push token and preload me`() = runTest {
        fakeChatEngine.givenLogin(A_LOGIN_REQUEST).returns(A_LOGIN_SUCCESS)
        fakePushTokenRegistrar.expect { it.registerCurrentToken() }
        fakeChatEngine.expect { it.me(forceRefresh = false) }

        val result = useCase.login(A_LOGIN_REQUEST)

        result shouldBeEqualTo A_LOGIN_SUCCESS
    }

    @Test
    fun `when logging in fails with MissingWellKnown, then does nothing`() = runTest {
        fakeChatEngine.givenLogin(A_LOGIN_REQUEST).returns(LoginResult.MissingWellKnown)

        val result = useCase.login(A_LOGIN_REQUEST)

        result shouldBeEqualTo LoginResult.MissingWellKnown
    }

    @Test
    fun `when logging in errors, then tracks cause`() = runTest {
        fakeChatEngine.givenLogin(A_LOGIN_REQUEST).returns(A_LOGIN_ERROR)
        fakeErrorTracker.expect { it.track(A_LOGIN_ERROR.cause) }

        val result = useCase.login(A_LOGIN_REQUEST)

        result shouldBeEqualTo A_LOGIN_ERROR
    }
}

class FakePushTokenRegistrar : PushTokenRegistrar by mockk()

private fun aUserCredentials() = UserCredentials("ignored", HomeServerUrl("ignored"), aUserId(), DeviceId("ignored"))
