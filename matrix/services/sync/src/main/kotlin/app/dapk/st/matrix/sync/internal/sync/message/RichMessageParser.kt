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
        val builder = PartBuilder()
        var nextIndex = 0
        while (nextIndex != END_SEARCH) {
            val htmlResult = htmlParser.parseHtmlTags(input, nextIndex, builder)
            val linkStartIndex = findUrlStartIndex(htmlResult, nextIndex)
            val urlResult = urlParser.parseUrl(input, linkStartIndex, builder)

            val hasReachedEnd = hasReachedEnd(htmlResult, urlResult, input)
            if (hasReachedEnd && hasUnprocessedText(htmlResult, urlResult, input)) {
                builder.appendText(input.substring(nextIndex))
            }
            nextIndex = if (hasReachedEnd) END_SEARCH else max(htmlResult, urlResult)
        }
        return RichText(builder.build())
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

private fun String.removeHtmlEntities() = this.replace("&quot;", "\"").replace("&#39;", "'")

private fun String.dropTextFallback() = this.lines()
    .dropWhile { it.startsWith("> ") || it.isEmpty() }
    .joinToString(separator = "\n")
