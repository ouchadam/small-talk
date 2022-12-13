package app.dapk.st.login.state.fakes

import app.dapk.st.engine.LoginRequest
import app.dapk.st.login.state.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeLoginUseCase {

    val instance = mockk<LoginUseCase>()

    fun given(loginRequest: LoginRequest) = coEvery { instance.login(loginRequest) }.delegateReturn()

}