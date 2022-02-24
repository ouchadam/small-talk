package app.dapk.st.core.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class Scope(
    dispatcher: CoroutineDispatcher
) {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(dispatcher + job)

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return coroutineScope.launch(context, start, block)
    }

    fun cancel() {
        job.cancel()
    }
}