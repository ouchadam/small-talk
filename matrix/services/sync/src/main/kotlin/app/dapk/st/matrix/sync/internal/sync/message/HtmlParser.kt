package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.UserId

private const val TAG_OPEN = '<'
private const val TAG_CLOSE = '>'
private const val NO_RESULT_FOUND = -1
private val SKIPPED_TAGS = setOf("mx-reply")

internal class HtmlParser {

    fun test(startingFrom: Int, input: String) = input.indexOf(TAG_OPEN, startingFrom)

    fun parseHtmlTags(input: String, searchIndex: Int, builder: PartBuilder, nestingLevel: Int = 0): SearchIndex = input.findTag(
        fromIndex = searchIndex,
        onInvalidTag = { builder.appendText(input[it].toString()) },
        onTag = { tagOpen, tagClose ->
            val (wholeTag, tagName) = parseTag(input, tagOpen, tagClose)

            when {
                tagName.startsWith('@') -> {
                    appendTextBeforeTag(searchIndex, tagOpen, builder, input)
                    builder.appendPerson(UserId(tagName), tagName)
                    tagClose.next()
                }

                tagName == "br" -> {
                    appendTextBeforeTag(searchIndex, tagOpen, builder, input)
                    builder.appendNewline()
                    tagClose.next()
                }

                else -> parseTagWithContent(input, tagName, tagClose, searchIndex, tagOpen, wholeTag, builder, nestingLevel)
            }
        }
    )

    private fun parseTagWithContent(
        input: String,
        tagName: String,
        tagClose: Int,
        searchIndex: Int,
        tagOpen: Int,
        wholeTag: String,
        builder: PartBuilder,
        nestingLevel: Int
    ): Int {
        val exitTag = "</$tagName>"
        val exitIndex = input.indexOf(exitTag, startIndex = tagClose)
        val exitTagCloseIndex = exitIndex + exitTag.length
        return when {
            exitIndex == NO_RESULT_FOUND -> {
                builder.appendText(input[searchIndex].toString())
                searchIndex.next()
            }

            SKIPPED_TAGS.contains(tagName) -> exitTagCloseIndex

            else -> {
                appendTextBeforeTag(searchIndex, tagOpen, builder, input)
                val tagContent = input.substring(tagClose + 1, exitIndex)
                handleTagWithContent(input, tagName, wholeTag, builder, tagContent, exitTagCloseIndex, nestingLevel)
            }
        }
    }

    private fun handleTagWithContent(
        input: String,
        tagName: String,
        wholeTag: String,
        builder: PartBuilder,
        tagContent: String,
        exitTagCloseIndex: Int,
        nestingLevel: Int,
    ) = when (tagName) {
        "a" -> {
            val findHrefUrl = wholeTag.findTagAttribute("href")
            when {
                findHrefUrl == null -> {
                    builder.appendText(tagContent)
                    exitTagCloseIndex
                }

                findHrefUrl.startsWith("https://matrix.to/#/@") -> {
                    val userId = UserId(findHrefUrl.substringAfter("https://matrix.to/#/").substringBeforeLast("\""))
                    builder.appendPerson(userId, "@${tagContent.removePrefix("@")}")
                    ignoreMatrixColonMentionSuffix(input, exitTagCloseIndex)
                }

                else -> {
                    builder.appendLink(findHrefUrl, label = tagContent)
                    exitTagCloseIndex
                }
            }
        }

        "b", "strong" -> {
            builder.appendBold(tagContent)
            exitTagCloseIndex
        }

        "p" -> {
            if (tagContent.isNotEmpty() && nestingLevel < 2) {
                var lastIndex = 0
                iterateSearchIndex { searchIndex ->
                    lastIndex = searchIndex
                    parseHtmlTags(tagContent, searchIndex, builder, nestingLevel = nestingLevel + 1)
                }

                if (lastIndex < tagContent.length) {
                    builder.appendText(tagContent.substring(lastIndex))
                }
            }

            builder.appendNewline()
            exitTagCloseIndex
        }

        "ul", "ol" -> {
            parseList(tagName, tagContent, builder)
            exitTagCloseIndex
        }

        "h1", "h2", "h3", "h4", "h5" -> {
            builder.appendBold(tagContent.trim())
            builder.appendNewline()
            exitTagCloseIndex
        }

        "i", "em" -> {
            builder.appendItalic(tagContent)
            exitTagCloseIndex
        }

        else -> {
            builder.appendText(tagContent)
            exitTagCloseIndex
        }
    }

    private fun ignoreMatrixColonMentionSuffix(input: String, exitTagCloseIndex: Int) = if (input.getOrNull(exitTagCloseIndex) == ':') {
        exitTagCloseIndex.next()
    } else {
        exitTagCloseIndex
    }

    private fun appendTextBeforeTag(searchIndex: Int, tagOpen: Int, builder: PartBuilder, input: String) {
        if (searchIndex != tagOpen) {
            builder.appendText(input.substring(searchIndex, tagOpen))
        }
    }

    private fun String.findTag(fromIndex: Int, onInvalidTag: (Int) -> Unit, onTag: (Int, Int) -> Int): Int {
        return when (val foundIndex = this.indexOf(TAG_OPEN, startIndex = fromIndex)) {
            NO_RESULT_FOUND -> END_SEARCH

            else -> when (val closeIndex = indexOf(TAG_CLOSE, startIndex = foundIndex)) {
                NO_RESULT_FOUND -> {
                    onInvalidTag(fromIndex)
                    fromIndex + 1
                }

                else -> onTag(foundIndex, closeIndex)
            }
        }
    }

    private fun parseList(parentTag: String, parentContent: String, builder: PartBuilder) {
        var listIndex = 1
        iterateSearchIndex { nextIndex ->
            singleTagParser(parentContent, "li", nextIndex, builder) { wholeTag, tagContent ->
                val content = when (parentTag) {
                    "ol" -> {
                        listIndex = wholeTag.findTagAttribute("value")?.toInt() ?: listIndex
                        "$listIndex. $tagContent".also { listIndex++ }
                    }

                    else -> "- $tagContent"
                }
                builder.appendText(content)
                builder.appendNewline()
            }
        }
    }

    private fun singleTagParser(content: String, wantedTagName: String, searchIndex: Int, builder: PartBuilder, onTag: (String, String) -> Unit): SearchIndex {
        return content.findTag(
            fromIndex = searchIndex,
            onInvalidTag = { builder.appendText(content[it].toString()) },
            onTag = { tagOpen, tagClose ->
                val (wholeTag, tagName) = parseTag(content, tagOpen, tagClose)

                if (tagName == wantedTagName) {
                    val exitTag = "</$tagName>"
                    val exitIndex = content.indexOf(exitTag, startIndex = tagClose)
                    val exitTagCloseIndex = exitIndex + exitTag.length
                    if (exitIndex == END_SEARCH) {
                        builder.appendText(content[searchIndex].toString())
                        searchIndex.next()
                    } else {
                        val tagContent = content.substring(tagClose + 1, exitIndex)
                        onTag(wholeTag, tagContent)
                        exitTagCloseIndex
                    }
                } else {
                    END_SEARCH
                }
            }
        )
    }

    private fun parseTag(input: String, tagOpen: Int, tagClose: Int): Pair<String, String> {
        val wholeTag = input.substring(tagOpen, tagClose + 1)
        val tagName = wholeTag.substring(1, wholeTag.indexOfFirst { it == '>' || it == ' ' })
        return wholeTag to tagName
    }
}

private fun String.findTagAttribute(name: String): String? {
    val attribute = "$name="
    return this.indexOf(attribute).let {
        if (it == NO_RESULT_FOUND) {
            null
        } else {
            val start = it + attribute.length
            this.substring(start).substringAfter('\"').substringBefore('\"')
        }
    }
}
