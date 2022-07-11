import app.dapk.st.viewmodel.MutableStateFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import test.ExpectTest

@Suppress("UNCHECKED_CAST")
class ViewModelTest {

    var instance: TestMutableState<Any>? = null

    fun <S> testMutableStateFactory(): MutableStateFactory<S> {
        return { TestMutableState(it).also { instance = it as TestMutableState<Any> } }
    }

    operator fun invoke(block: suspend ViewModelTestScope.() -> Unit) {
        runTest {
            val expectTest = ExpectTest(coroutineContext)
            val viewModelTest = ViewModelTestScopeImpl(expectTest, this@ViewModelTest)
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            block(viewModelTest)
            viewModelTest.finish()
            Dispatchers.resetMain()
        }
    }
}
