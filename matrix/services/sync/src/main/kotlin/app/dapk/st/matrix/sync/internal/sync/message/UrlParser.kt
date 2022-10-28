package app.dapk.st.matrix.sync.internal.sync.message

private const val INVALID_TRAILING_CHARS = ",.:;?<>"

internal class UrlParser {

    private fun String.hasLookAhead(current: Int, value: String): Boolean {
        return length > current + value.length && this.substring(current, current + value.length) == value
    }

    fun parseUrl(input: String, linkStartIndex: Int, builder: PartBuilder): Int {
        val urlIndex = input.indexOf("http", startIndex = linkStartIndex)
        return if (urlIndex == END_SEARCH) END_SEARCH else {
            builder.appendTextBeforeTag(linkStartIndex, urlIndex, input)

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
                    builder.appendLink(url = cleanedUrl, label = null)
                    if (cleanedUrl != originalUrl) {
                        builder.appendText(originalUrl.last().toString())
                    }
                    input.length.next()
                }

                else -> {
                    val originalUrl = input.substring(urlIndex, urlEndIndex)
                    val cleanedUrl = originalUrl.bestGuessStripTrailingUrlChar()
                    builder.appendLink(url = cleanedUrl, label = null)
                    if (originalUrl == cleanedUrl) urlEndIndex else urlEndIndex - 1
                }
            }
        }
    }

    fun test(startingFrom: Int, input: String): Int {
        return input.indexOf("http", startingFrom)
    }

}


private fun String.bestGuessStripTrailingUrlChar(): String {
    val last = this.last()
    return if (INVALID_TRAILING_CHARS.contains(last)) {
        this.dropLast(1)
    } else {
        this
    }
}
