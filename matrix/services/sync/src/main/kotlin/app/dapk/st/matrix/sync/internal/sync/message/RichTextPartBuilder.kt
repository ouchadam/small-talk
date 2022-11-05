package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.UserId

interface ContentAccumulator {
    fun appendText(value: String)
    fun appendItalic(value: String)
    fun appendBold(value: String)
    fun appendPerson(userId: UserId, displayName: String)
    fun appendLink(url: String, label: String?)
    fun build(): List<RichText.Part>
}

class RichTextPartBuilder : ContentAccumulator {

    private var normalBuffer = StringBuilder()

    private val parts = mutableListOf<RichText.Part>()

    override fun appendText(value: String) {
        normalBuffer.append(value.cleanFirstTextLine())
    }

    override fun appendItalic(value: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Italic(value.cleanFirstTextLine()))
    }

    override fun appendBold(value: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Bold(value.cleanFirstTextLine()))
    }

    private fun String.cleanFirstTextLine() = if (parts.isEmpty() && normalBuffer.isEmpty()) this.trimStart() else this

    override fun appendPerson(userId: UserId, displayName: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Person(userId, displayName))
    }

    override fun appendLink(url: String, label: String?) {
        flushNormalBuffer()
        parts.add(RichText.Part.Link(url, label ?: url))
    }

    override fun build(): List<RichText.Part> {
        flushNormalBuffer()
        return when (parts.isEmpty()) {
            true -> parts
            else -> {
                val last = parts.last()
                if (last is RichText.Part.Normal) {
                    parts.removeLast()
                    val newContent = last.content.trimEnd()
                    if (newContent.isNotEmpty()) {
                        parts.add(last.copy(content = newContent))
                    }
                }
                parts
            }
        }
    }

    private fun flushNormalBuffer() {
        if (normalBuffer.isNotEmpty()) {
            parts.add(RichText.Part.Normal(normalBuffer.toString()))
            normalBuffer.clear()
        }
    }
}

internal fun ContentAccumulator.appendNewline() {
    this.appendText("\n")
}

