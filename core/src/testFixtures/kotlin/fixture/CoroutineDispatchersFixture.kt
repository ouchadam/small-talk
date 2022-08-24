package fixture

import app.dapk.st.core.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object CoroutineDispatchersFixture {

    fun aCoroutineDispatchers() = CoroutineDispatchers(
        Dispatchers.Unconfined,
        main = Dispatchers.Unconfined,
        global = CoroutineScope(Dispatchers.Unconfined)
    )
}