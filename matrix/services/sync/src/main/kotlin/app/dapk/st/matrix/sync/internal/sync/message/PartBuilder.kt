package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.UserId

internal class PartBuilder {

    private var normalBuffer = StringBuilder()

    private val parts = mutableListOf<RichText.Part>()

    fun appendText(value: String) {
        normalBuffer.append(value.cleanFirstTextLine())
    }

    fun appendItalic(value: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Italic(value.cleanFirstTextLine()))
    }

    fun appendBold(value: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Bold(value.cleanFirstTextLine()))
    }

    private fun String.cleanFirstTextLine() = if (parts.isEmpty() && normalBuffer.isEmpty()) this.trimStart() else this

    fun appendPerson(userId: UserId, displayName: String) {
        flushNormalBuffer()
        parts.add(RichText.Part.Person(userId, displayName))
    }

    fun appendLink(url: String, label: String?) {
        flushNormalBuffer()
        parts.add(RichText.Part.Link(url, label ?: url))
    }

    fun build(): List<RichText.Part> {
        flushNormalBuffer()
        return when(parts.isEmpty()) {
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

internal fun PartBuilder.appendTextBeforeTag(previousIndex: Int, tagOpenIndex: Int, input: String) {
    if (previousIndex != tagOpenIndex) {
        this.appendText(input.substring(previousIndex, tagOpenIndex))
    }
}

internal fun PartBuilder.appendNewline() {
    this.appendText("\n")
}

