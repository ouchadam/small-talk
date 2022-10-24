package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RichText.Part.*
import app.dapk.st.matrix.common.UserId

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
                            println(tagName)
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

                        println("$exitTag : $exitIndex")

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

                        val substring1 = input.substring(urlIndex)
                        val urlEndIndex = substring1.indexOfFirst { it == '\n' || it == ' ' }
                        when {
                            urlEndIndex == -1 -> {
                                val last = substring1.last()
                                val url = substring1.removeSuffix(".").removeSuffix(",")
                                buffer.add(Link(url = url, label = url))
                                if (last == '.' || last == ',') {
                                    buffer.add(Normal(last.toString()))
                                }
                                break
                            }

                            else -> {
                                val substring = input.substring(urlIndex, urlEndIndex)
                                val url = substring.removeSuffix(".").removeSuffix(",")
                                buffer.add(Link(url = url, label = url))
                                openIndex = if (substring.endsWith('.') || substring.endsWith(',')) urlEndIndex - 1 else urlEndIndex
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

private fun String.dropTextFallback() = this.lines().dropWhile { it.startsWith("> ") || it.isEmpty() }.joinToString("")
