package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.RichText
import kotlin.math.max

internal const val END_SEARCH = -1

class RichMessageParser {

    private val htmlParser = HtmlParser()
    private val urlParser = UrlParser()

    fun parse(source: String): RichText {
        val input = source
            .removeHtmlEntities()
            .dropTextFallback()
        return RichText(collectRichText(input).build())
    }

    private fun collectRichText(input: String) = PartBuilder().also { builder ->
        iterateSearchIndex { nextIndex ->
            val htmlStart = htmlParser.test(nextIndex, input)
            val urlStart = urlParser.test(nextIndex, input)

            val firstResult = if (htmlStart < urlStart) {
                htmlParser.parseHtmlTags(input, nextIndex, builder)
            } else {
                urlParser.parseUrl(input, nextIndex, builder)
            }

            val secondStartIndex = findUrlStartIndex(firstResult, nextIndex)
            val secondResult = if (htmlStart < urlStart) {
                urlParser.parseUrl(input, secondStartIndex, builder)
            } else {
                htmlParser.parseHtmlTags(input, secondStartIndex, builder)
            }

            val hasReachedEnd = hasReachedEnd(firstResult, secondResult, input)
            if (hasReachedEnd && hasUnprocessedText(firstResult, secondResult, input)) {
                builder.appendText(input.substring(nextIndex))
            }
            if (hasReachedEnd) END_SEARCH else max(firstResult, secondResult)
        }
    }

    private fun hasUnprocessedText(htmlResult: Int, urlResult: Int, input: String) = htmlResult < input.length && urlResult < input.length

    private fun findUrlStartIndex(htmlResult: Int, searchIndex: Int) = when {
        htmlResult == END_SEARCH && searchIndex == 0 -> 0
        htmlResult == END_SEARCH -> searchIndex
        else -> htmlResult
    }

    private fun hasReachedEnd(htmlResult: SearchIndex, urlResult: Int, input: String) =
        (htmlResult == END_SEARCH && urlResult == END_SEARCH) || (htmlResult >= input.length || urlResult >= input.length)

}

private fun String.removeHtmlEntities() = this.replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")

private fun String.dropTextFallback() = this.lines()
    .dropWhile { it.startsWith("> ") || it.isEmpty() }
    .joinToString(separator = "\n")

internal fun iterateSearchIndex(action: (SearchIndex) -> SearchIndex): SearchIndex {
    var nextIndex = 0
    while (nextIndex != END_SEARCH) {
        nextIndex = action(nextIndex)
    }
    return nextIndex
}