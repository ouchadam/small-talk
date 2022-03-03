package internalfake

import app.dapk.st.matrix.crypto.internal.FetchAccountCryptoUseCase
import io.mockk.coEvery
import io.mockk.mockk
import test.delegateReturn

class FakeFetchAccountCryptoUseCase : FetchAccountCryptoUseCase by mockk() {
    fun givenFetch() = coEvery { this@FakeFetchAccountCryptoUseCase.invoke() }.delegateReturn()
}