package test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.amshove.kluent.internal.assertEquals

class FlowTestObserver<T>(scope: CoroutineScope, flow: Flow<T>) {

    private val values = mutableListOf<T>()
    private val job: Job = flow
        .onEach { values.add(it) }
        .launchIn(scope)

    fun assertValues(values: List<T>) = assertEquals(values, this.values)
    fun finish() = job.cancel()
}