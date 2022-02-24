package app.dapk.st.matrix.sync.internal.filter

import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.matrix.sync.internal.request.ApiFilterResponse
import app.dapk.st.matrix.sync.internal.request.FilterRequest
import fake.FakeFilterStore
import fake.FakeMatrixHttpClient
import fixture.aUserId
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.expect

private const val A_FILTER_KEY = "a-filter-key"
private const val A_FILTER_ID_VALUE = "a-filter-id"
private val A_FILTER_REQUEST = FilterRequest()

internal class FilterUseCaseTest {

    private val fakeClient = FakeMatrixHttpClient()
    private val fakeFilterStore = FakeFilterStore()

    private val filterUseCase = FilterUseCase(fakeClient, fakeFilterStore)

    @Test
    fun `given cached filter then returns cached value`() = runTest {
        fakeFilterStore.givenCachedFilter(A_FILTER_KEY, A_FILTER_ID_VALUE)

        val result = filterUseCase.filter(A_FILTER_KEY, aUserId(), A_FILTER_REQUEST)

        result shouldBeEqualTo SyncService.FilterId(A_FILTER_ID_VALUE)
    }

    @Test
    fun `given no cached filter then fetches upstream filter and caches id result`() = runTest {
        fakeFilterStore.givenCachedFilter(A_FILTER_KEY, filterIdValue = null)
        fakeFilterStore.expect { it.store(A_FILTER_KEY, A_FILTER_ID_VALUE) }
        fakeClient.given(request = filterRequest(aUserId(), A_FILTER_REQUEST), response = ApiFilterResponse(A_FILTER_ID_VALUE))

        val result = filterUseCase.filter(A_FILTER_KEY, aUserId(), A_FILTER_REQUEST)

        result shouldBeEqualTo SyncService.FilterId(A_FILTER_ID_VALUE)
    }
}
