package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RichText.Part.*
import app.dapk.st.matrix.common.UserId

private const val INVALID_TRAILING_CHARS = ",.:;?"
private const val TAG_OPEN = '<'
private const val TAG_CLOSE = '>'

class RichMessageParser {

    fun parse(source: String): RichText {
        val input = source
            .removeHtmlEntities()
            .dropTextFallback()
        val builder = PartBuilder()
        var openIndex = 0
        var closeIndex = 0
        var lastStartIndex = 0
        while (openIndex != -1) {
            val foundIndex = input.indexOf(TAG_OPEN, startIndex = openIndex)
            if (foundIndex != -1) {
                closeIndex = input.indexOf(TAG_CLOSE, startIndex = foundIndex)
                if (closeIndex == -1) {
                    openIndex++
                } else {
                    val wholeTag = input.substring(foundIndex, closeIndex + 1)
                    val tagName = wholeTag.substring(1, wholeTag.indexOfFirst { it == '>' || it == ' ' })

                    if (tagName.startsWith('@')) {
                        if (openIndex != foundIndex) {
                            builder.appendText(input.substring(openIndex, foundIndex))
                        }
                        builder.appendPerson(UserId(tagName), tagName)
                        openIndex = foundIndex + wholeTag.length
                        lastStartIndex = openIndex
                        continue
                    }

                    if (tagName == "br") {
                        if (openIndex != foundIndex) {
                            builder.appendText(input.substring(openIndex, foundIndex))
                        }
                        builder.appendText("\n")
                        openIndex = foundIndex + wholeTag.length
                        lastStartIndex = openIndex
                        continue
                    }

                    val exitTag = "</$tagName>"
                    val exitIndex = input.indexOf(exitTag, startIndex = closeIndex)
                    if (exitIndex == -1) {
                        openIndex++
                    } else {
                        when (tagName) {
                            "mx-reply" -> {
                                openIndex = exitIndex + exitTag.length
                                lastStartIndex = openIndex
                                continue
                            }
                        }

                        if (openIndex != foundIndex) {
                            builder.appendText(input.substring(openIndex, foundIndex))
                        }
                        val tagContent = input.substring(closeIndex + 1, exitIndex)
                        openIndex = exitIndex + exitTag.length
                        lastStartIndex = openIndex

                        when (tagName) {
                            "a" -> {
                                val findHrefUrl = wholeTag.substringAfter("href=").replace("\"", "").removeSuffix(">")
                                if (findHrefUrl.startsWith("https://matrix.to/#/@")) {
                                    val userId = UserId(findHrefUrl.substringAfter("https://matrix.to/#/").substringBeforeLast("\""))
                                    builder.appendPerson(userId, "@${tagContent.removePrefix("@")}")
                                    if (input.getOrNull(openIndex) == ':') {
                                        openIndex++
                                        lastStartIndex = openIndex
                                    }
                                } else {
                                    builder.appendLink(findHrefUrl, label = tagContent)
                                }
                            }

                            "b" -> builder.appendBold(tagContent)
                            "strong" -> builder.appendBold(tagContent)
                            "i" -> builder.appendItalic(tagContent)
                            "em" -> builder.appendItalic(tagContent)

                            else -> builder.appendText(tagContent)
                        }
                    }
                }
            } else {
                // check for urls
                val urlIndex = input.indexOf("http", startIndex = openIndex)
                if (urlIndex != -1) {
                    if (lastStartIndex != urlIndex) {
                        builder.appendText(input.substring(lastStartIndex, urlIndex))
                    }

                    val originalUrl = input.substring(urlIndex)
                    val urlEndIndex = originalUrl.indexOfFirst { it == '\n' || it == ' ' }
                    val urlContinuesUntilEnd = urlEndIndex == -1
                    when {
                        urlContinuesUntilEnd -> {
                            val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                            builder.appendLink(url = cleanedUrl, label = null)
                            if (cleanedUrl != originalUrl) {
                                builder.appendText(originalUrl.last().toString())
                            }
                            break
                        }

                        else -> {
                            val originalUrl = input.substring(urlIndex, urlEndIndex)
                            val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                            builder.appendLink(url = cleanedUrl, label = null)
                            openIndex = if (originalUrl == cleanedUrl) urlEndIndex else urlEndIndex - 1
                            lastStartIndex = openIndex
                            continue
                        }
                    }
                }

                // exit
                if (lastStartIndex < input.length) {
                    builder.appendText(input.substring(lastStartIndex))
                }
                break
            }
        }
        return RichText(builder.build())
    }
}

private fun String.removeHtmlEntities() = this.replace("&quot;", "\"").replace("&#39;", "'")

private fun String.dropTextFallback() = this.lines()
    .dropWhile { it.startsWith("> ") || it.isEmpty() }
    .joinToString(separator = "\n")

private fun String.bestGuessStripTrailingUrlChar(): String {
    val last = this.last()
    return if (INVALID_TRAILING_CHARS.contains(last)) {
        this.dropLast(1)
    } else {
        this
    }
}

private class PartBuilder {

    private var normalBuffer = StringBuilder()

    private val parts = mutableSetOf<RichText.Part>()

    fun appendText(value: String) {
        normalBuffer.append(value.cleanFirstTextLine())
    }

    fun appendItalic(value: String) {
        flushNormalBuffer()
        parts.add(Italic(value.cleanFirstTextLine()))
    }

    fun appendBold(value: String) {
        flushNormalBuffer()
        parts.add(Bold(value.cleanFirstTextLine()))
    }

    private fun String.cleanFirstTextLine() = if (parts.isEmpty() && normalBuffer.isEmpty()) this.trimStart() else this

    fun appendPerson(userId: UserId, displayName: String) {
        flushNormalBuffer()
        parts.add(Person(userId, displayName))
    }

    fun appendLink(url: String, label: String?) {
        flushNormalBuffer()
        parts.add(Link(url, label ?: url))
    }

    fun build(): Set<RichText.Part> {
        flushNormalBuffer()
        return parts
    }

    private fun flushNormalBuffer() {
        if (normalBuffer.isNotEmpty()) {
            parts.add(Normal(normalBuffer.toString()))
            normalBuffer.clear()
        }
    }

}