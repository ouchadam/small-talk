package app.dapk.st.matrix.sync.internal.sync.message

private const val INVALID_TRAILING_CHARS = ",.:;?"

internal class UrlParser {

    fun parseUrl(input: String, linkStartIndex: Int, builder: PartBuilder): Int {
        val urlIndex = input.indexOf("http", startIndex = linkStartIndex)
        val urlResult = if (urlIndex == END_SEARCH) END_SEARCH else {
            builder.appendTextBeforeTag(linkStartIndex, urlIndex, input)

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
        return urlResult
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
