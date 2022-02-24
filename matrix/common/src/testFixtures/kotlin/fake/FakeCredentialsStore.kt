package fake

import app.dapk.st.matrix.common.CredentialsStore
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeCredentialsStore : CredentialsStore by mockk() {
    fun givenCredentials() = coEvery { credentials() }.delegateReturn()
}