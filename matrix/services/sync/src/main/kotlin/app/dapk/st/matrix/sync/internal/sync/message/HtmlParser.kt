package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.UserId

private const val TAG_OPEN = '<'
private const val TAG_CLOSE = '>'
private const val NO_RESULT_FOUND = -1

internal class HtmlParser {

    fun parseHtmlTags(input: String, searchIndex: Int, builder: PartBuilder) = input.findTag(
        fromIndex = searchIndex,
        onInvalidTag = { builder.appendText(input[it].toString()) },
        onTag = { tagOpen, tagClose ->
            val wholeTag = input.substring(tagOpen, tagClose + 1)
            val tagName = wholeTag.substring(1, wholeTag.indexOfFirst { it == '>' || it == ' ' })

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

                else -> {
                    val exitTag = "</$tagName>"
                    val exitIndex = input.indexOf(exitTag, startIndex = tagClose)
                    val exitTagCloseIndex = exitIndex + exitTag.length
                    if (exitIndex == END_SEARCH) {
                        builder.appendText(input[searchIndex].toString())
                        searchIndex.next()
                    } else {
                        when (tagName) {
                            "mx-reply" -> {
                                exitTagCloseIndex
                            }

                            else -> {
                                appendTextBeforeTag(searchIndex, tagOpen, builder, input)
                                val tagContent = input.substring(tagClose + 1, exitIndex)
                                handleTagWithContent(input, tagName, wholeTag, builder, tagContent, exitTagCloseIndex)
                            }
                        }
                    }
                }
            }
        }
    )

    private fun handleTagWithContent(
        input: String,
        tagName: String,
        wholeTag: String,
        builder: PartBuilder,
        tagContent: String,
        exitTagCloseIndex: Int
    ) = when (tagName) {
        "a" -> {
            val findHrefUrl = wholeTag.substringAfter("href=").replace("\"", "").removeSuffix(">")
            if (findHrefUrl.startsWith("https://matrix.to/#/@")) {
                val userId = UserId(findHrefUrl.substringAfter("https://matrix.to/#/").substringBeforeLast("\""))
                builder.appendPerson(userId, "@${tagContent.removePrefix("@")}")
                ignoreMatrixColonMentionSuffix(input, exitTagCloseIndex)
            } else {
                builder.appendLink(findHrefUrl, label = tagContent)
                exitTagCloseIndex
            }
        }

        "b", "strong" -> {
            builder.appendBold(tagContent)
            exitTagCloseIndex
        }

        "p" -> {
            builder.appendText(tagContent)
            builder.appendNewline()
            exitTagCloseIndex
        }

        "h1", "h2", "h3", "h4", "h5" -> {
            builder.appendBold(tagContent)
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

    fun test(startingFrom: Int, intput: String): Int {
        return intput.indexOf('<', startingFrom)
    }

}