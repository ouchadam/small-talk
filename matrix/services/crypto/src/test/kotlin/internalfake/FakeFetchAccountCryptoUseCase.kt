package internalfake

import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.internal.FetchAccountCryptoUseCase
import io.mockk.coEvery
import io.mockk.mockk

class FakeFetchAccountCryptoUseCase : FetchAccountCryptoUseCase by mockk() {
    fun givenAccount(account: Olm.AccountCryptoSession) {
        coEvery { this@FakeFetchAccountCryptoUseCase.invoke() } returns account
    }
}