package app.dapk.st.matrix.sync.internal.sync.message.url

import app.dapk.st.matrix.sync.internal.sync.message.ContentAccumulator

private const val END_SEARCH = -1
private const val INVALID_TRAILING_CHARS = ",.:;?<>"

internal class UrlParser {

    fun parseUrl(input: String, urlIndex: Int, accumulator: ContentAccumulator): Int {
        return if (urlIndex == END_SEARCH) END_SEARCH else {
            val originalUrl = input.substring(urlIndex)
            var index = 0
            val maybeUrl = originalUrl.takeWhile {
                it != '\n' && it != ' ' && !originalUrl.hasLookAhead(index++, "<br")
            }

            val urlEndIndex = maybeUrl.length + urlIndex
            val urlContinuesUntilEnd = urlEndIndex == -1

            when {
                urlContinuesUntilEnd -> {
                    val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                    accumulator.appendLink(url = cleanedUrl, label = null)
                    if (cleanedUrl != originalUrl) {
                        accumulator.appendText(originalUrl.last().toString())
                    }
                    input.length + 1
                }

                else -> {
                    val originalUrl = input.substring(urlIndex, urlEndIndex)
                    val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                    accumulator.appendLink(url = cleanedUrl, label = null)
                    if (originalUrl == cleanedUrl) urlEndIndex else urlEndIndex - 1
                }
            }
        }
    }
}

private fun String.hasLookAhead(current: Int, value: String): Boolean {
    return length > current + value.length && this.substring(current, current + value.length) == value
}

private fun String.bestGuessStripTrailingUrlChar(): String {
    val last = this.last()
    return if (INVALID_TRAILING_CHARS.contains(last)) {
        this.dropLast(1)
    } else {
        this
    }
}
