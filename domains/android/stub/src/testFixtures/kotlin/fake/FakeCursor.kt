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

interface CreateCursorScope {
    fun addRow(vararg item: Pair<String, Any?>)
}

fun createCursor(creator: CreateCursorScope.() -> Unit): Cursor {
    val content = mutableListOf<Map<String, Any?>>()
    val scope = object : CreateCursorScope {
        override fun addRow(vararg item: Pair<String, Any?>) {
            content.add(item.toMap())
        }
    }
    creator(scope)
    return StubCursor(content)
}

private class StubCursor(private val content: List<Map<String, Any?>>) : Cursor by mockk() {

    private val columnNames = content.map { it.keys }.flatten().distinct()
    private var currentRowIndex = -1

    override fun getColumnIndexOrThrow(columnName: String): Int {
        return getColumnIndex(columnName).takeIf { it != -1 } ?: throw IllegalArgumentException(columnName)
    }

    override fun getColumnIndex(columnName: String) = columnNames.indexOf(columnName)

    override fun moveToNext() = (currentRowIndex + 1 < content.size).also {
        currentRowIndex += 1
    }

    override fun moveToFirst() = content.isNotEmpty()

    override fun getCount() = content.size

    override fun getString(index: Int): String? = content[currentRowIndex][columnNames[index]] as? String

    override fun getInt(index: Int): Int {
        return content[currentRowIndex][columnNames[index]] as? Int ?: throw IllegalArgumentException("Int can't be null")
    }

    override fun getLong(index: Int): Long {
        return content[currentRowIndex][columnNames[index]] as? Long ?: throw IllegalArgumentException("Long can't be null")
    }

    override fun getColumnCount() = columnNames.size

    override fun close() {
        // do nothing
    }
}