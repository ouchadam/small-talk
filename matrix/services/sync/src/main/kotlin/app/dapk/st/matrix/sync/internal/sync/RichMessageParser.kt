package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RichText.Part.*
import app.dapk.st.matrix.common.UserId

private const val INVALID_TRAILING_CHARS = ",.:;?"

class RichMessageParser {

    fun parse(source: String): RichText {
        val input = source
            .removeHtmlEntities()
            .dropTextFallback()
        return kotlin.runCatching {
            val buffer = mutableSetOf<RichText.Part>()
            var openIndex = 0
            var closeIndex = 0
            var lastStartIndex = 0
            while (openIndex != -1) {
                val foundIndex = input.indexOf('<', startIndex = openIndex)
                if (foundIndex != -1) {
                    closeIndex = input.indexOf('>', startIndex = foundIndex)
                    if (closeIndex == -1) {
                        openIndex++
                    } else {
                        val wholeTag = input.substring(foundIndex, closeIndex + 1)
                        val tagName = wholeTag.substring(1, wholeTag.indexOfFirst { it == '>' || it == ' ' })

                        if (tagName.startsWith('@')) {
                            if (openIndex != foundIndex) {
                                buffer.add(Normal(input.substring(openIndex, foundIndex)))
                            }
                            buffer.add(Person(UserId(tagName), tagName))
                            openIndex = foundIndex + wholeTag.length
                            lastStartIndex = openIndex
                            continue
                        }

                        if (tagName == "br") {
                            if (openIndex != foundIndex) {
                                buffer.add(Normal(input.substring(openIndex, foundIndex)))
                            }
                            buffer.add(Normal("\n"))
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
                                buffer.add(Normal(input.substring(openIndex, foundIndex)))
                            }
                            val tagContent = input.substring(closeIndex + 1, exitIndex)
                            openIndex = exitIndex + exitTag.length
                            lastStartIndex = openIndex

                            when (tagName) {
                                "a" -> {
                                    val findHrefUrl = wholeTag.substringAfter("href=").replace("\"", "").removeSuffix(">")
                                    if (findHrefUrl.startsWith("https://matrix.to/#/@")) {
                                        val userId = UserId(findHrefUrl.substringAfter("https://matrix.to/#/").substringBeforeLast("\""))
                                        buffer.add(Person(userId, "@${tagContent.removePrefix("@")}"))
                                        if (input.getOrNull(openIndex) == ':') {
                                            openIndex++
                                            lastStartIndex = openIndex
                                        }
                                    } else {
                                        buffer.add(Link(url = findHrefUrl, label = tagContent))
                                    }
                                }

                                "b" -> buffer.add(Bold(tagContent))
                                "strong" -> buffer.add(Bold(tagContent))
                                "i" -> buffer.add(Italic(tagContent))
                                "em" -> buffer.add(Italic(tagContent))

                                else -> buffer.add(Normal(tagContent))
                            }
                        }
                    }
                } else {
                    // check for urls
                    val urlIndex = input.indexOf("http", startIndex = openIndex)
                    if (urlIndex != -1) {
                        if (lastStartIndex != urlIndex) {
                            buffer.add(Normal(input.substring(lastStartIndex, urlIndex)))
                        }

                        val originalUrl = input.substring(urlIndex)
                        val urlEndIndex = originalUrl.indexOfFirst { it == '\n' || it == ' ' }
                        val urlContinuesUntilEnd = urlEndIndex == -1
                        when {
                            urlContinuesUntilEnd -> {
                                val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                                buffer.add(Link(url = cleanedUrl, label = cleanedUrl))
                                if (cleanedUrl != originalUrl) {
                                    buffer.add(Normal(originalUrl.last().toString()))
                                }
                                break
                            }

                            else -> {
                                val originalUrl = input.substring(urlIndex, urlEndIndex)
                                val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                                buffer.add(Link(url = cleanedUrl, label = cleanedUrl))
                                openIndex = if (originalUrl == cleanedUrl) urlEndIndex else urlEndIndex - 1
                                lastStartIndex = openIndex
                                continue
                            }
                        }
                    }

                    // exit
                    if (lastStartIndex < input.length) {
                        buffer.add(Normal(input.substring(lastStartIndex)))
                    }
                    break
                }
            }
            RichText(buffer)
        }.onFailure {
            it.printStackTrace()
            println(input)
        }.getOrThrow()
    }

}

private fun String.removeHtmlEntities() = this.replace("&quot;", "\"").replace("&#39;", "'")

private fun String.dropTextFallback() = this.lines().dropWhile { it.startsWith("> ") || it.isEmpty() }.joinToString("\n")

private fun String.bestGuessStripTrailingUrlChar(): String {
    val last = this.last()
    return if (INVALID_TRAILING_CHARS.contains(last)) {
        this.dropLast(1)
    } else {
        this
    }
}