package fake

import org.amshove.kluent.internal.assertEquals

class FakeEventSource<E> : (E) -> Unit {

    private val captures = mutableListOf<E>()

    override fun invoke(event: E) {
        captures.add(event)
    }

    fun assertEvents(expected: List<E>) {
        assertEquals(expected, captures)
    }

    fun assertNoEvents() {
        assertEquals(emptyList(), captures)
    }
}