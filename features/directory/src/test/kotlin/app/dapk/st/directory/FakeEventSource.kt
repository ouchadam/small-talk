package app.dapk.st.directory

import org.amshove.kluent.internal.assertEquals

class FakeEventSource<E> : (E) -> Unit {

    val captures = mutableListOf<E>()

    override fun invoke(event: E) {
        captures.add(event)
    }

    fun assertEvents(expected: List<E>) {
        assertEquals(expected, captures)
    }
}