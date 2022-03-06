package fake

import android.database.Cursor
import io.mockk.every
import io.mockk.mockk

class FakeCursor {

    val instance = mockk<Cursor>()

    init {
        every { instance.close() } answers {}
    }

    fun givenEmpty() {
        every { instance.count } returns 0
        every { instance.moveToFirst() } returns false
    }

    fun givenString(columnName: String, content: String?) {
        val columnId = columnName.hashCode()
        every { instance.moveToFirst() } returns true
        every { instance.isNull(columnId) } returns (content == null)
        every { instance.getColumnIndex(columnName) } returns columnId
        every { instance.getString(columnId) } returns content
    }
}