package fake

import app.dapk.st.matrix.sync.FilterStore
import io.mockk.coEvery
import io.mockk.mockk

class FakeFilterStore : FilterStore by mockk() {

    fun givenCachedFilter(key: String, filterIdValue: String?) {
        coEvery { read(key) } returns filterIdValue
    }

}
