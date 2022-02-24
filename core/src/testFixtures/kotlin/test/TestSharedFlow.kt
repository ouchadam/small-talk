package test

import kotlinx.coroutines.flow.MutableSharedFlow
import org.amshove.kluent.shouldBeEqualTo

class TestSharedFlow<T>(
    private val instance: MutableSharedFlow<T> = MutableSharedFlow()
) : MutableSharedFlow<T> by instance {

    private val values = mutableListOf<T>()

    override suspend fun emit(value: T) {
        values.add(value)
        instance.emit(value)
    }

    override fun tryEmit(value: T): Boolean {
        values.add(value)
        return instance.tryEmit(value)
    }

    fun assertNoValues() {
        values shouldBeEqualTo emptyList()
    }

    fun assertValues(vararg expected: T) {
        this.values shouldBeEqualTo expected.toList()
    }
}