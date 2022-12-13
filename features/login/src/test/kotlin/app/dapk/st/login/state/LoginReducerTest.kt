package app.dapk.st.login.state

import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.st.login.state.fakes.FakeLoginUseCase
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.matrix.common.UserCredentials
import fixture.aUserId
import org.junit.Test
import test.assertDispatches
import test.assertEvents
import test.assertOnlyDispatches
import test.testReducer

private val A_LOGIN_ACTION = LoginAction.Login(
    userName = "a-username",
    password = "a-password",
    serverUrl = "a-server-url",
)

private val AN_ERROR_CAUSE = RuntimeException()

class LoginReducerTest {

    private val fakeLoginUseCase = FakeLoginUseCase()

    private val runReducerTest = testReducer { events: (LoginEvent) -> Unit ->
        loginReducer(fakeLoginUseCase.instance, events)
    }

    @Test
    fun `initial state is idle without server url`() = runReducerTest {
        assertInitialState(LoginScreenState(showServerUrl = false, content = LoginScreenState.Content.Idle))
    }

    @Test
    fun `given non initial state, when Visible, then updates state to Idle with previous showServerUrl`() = runReducerTest {
        setState(LoginScreenState(showServerUrl = true, LoginScreenState.Content.Loading))

        reduce(LoginAction.ComponentLifecycle.Visible)

        assertOnlyStateChange(LoginScreenState(showServerUrl = true, LoginScreenState.Content.Idle))
    }

    @Test
    fun `when UpdateContent, then only updates content state`() = runReducerTest {
        reduce(LoginAction.UpdateContent(LoginScreenState.Content.Loading))

        assertOnlyStateChange {
            it.copy(content = LoginScreenState.Content.Loading)
        }
    }

    @Test
    fun `when UpdateState, then only updates state`() = runReducerTest {
        reduce(LoginAction.UpdateState(LoginScreenState(showServerUrl = true, LoginScreenState.Content.Loading)))

        assertOnlyStateChange(LoginScreenState(showServerUrl = true, LoginScreenState.Content.Loading))
    }

    @Test
    fun `given login errors, when Login, then updates content with loading and error`() = runReducerTest {
        fakeLoginUseCase.given(A_LOGIN_ACTION.toRequest()).returns(LoginResult.Error(AN_ERROR_CAUSE))

        reduce(A_LOGIN_ACTION)

        assertOnlyDispatches(
            LoginAction.UpdateContent(LoginScreenState.Content.Loading),
            LoginAction.UpdateContent(LoginScreenState.Content.Error(AN_ERROR_CAUSE)),
        )
    }

    @Test
    fun `given login fails with WellKnownMissing, when Login, then emits WellKnownMissing event and updates content with loading and Idle showing server url`() =
        runReducerTest {
            fakeLoginUseCase.given(A_LOGIN_ACTION.toRequest()).returns(LoginResult.MissingWellKnown)

            reduce(A_LOGIN_ACTION)

            assertDispatches(
                LoginAction.UpdateContent(LoginScreenState.Content.Loading),
                LoginAction.UpdateState(LoginScreenState(showServerUrl = true, LoginScreenState.Content.Idle)),
            )
            assertEvents(LoginEvent.WellKnownMissing)
            assertNoStateChange()
        }

    @Test
    fun `given login success, when Login, then emits LoginComplete event`() = runReducerTest {
        fakeLoginUseCase.given(A_LOGIN_ACTION.toRequest()).returns(LoginResult.Success(aUserCredentials()))

        reduce(A_LOGIN_ACTION)

        assertDispatches(LoginAction.UpdateContent(LoginScreenState.Content.Loading))
        assertEvents(LoginEvent.LoginComplete)
        assertNoStateChange()
    }
}

private fun LoginAction.Login.toRequest() = LoginRequest(this.userName, this.password, this.serverUrl)

private fun aUserCredentials() = UserCredentials("ignored", HomeServerUrl("ignored"), aUserId(), DeviceId("ignored"))