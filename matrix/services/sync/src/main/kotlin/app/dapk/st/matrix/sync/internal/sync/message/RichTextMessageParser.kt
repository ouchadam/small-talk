package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.common.RichText

fun interface NestedParser {
    fun parse(content: String, accumulator: ContentAccumulator)
}

fun interface TagParser {
    fun parse(tagName: String, attributes: Map<String, String>, content: String, accumulator: ContentAccumulator, parser: NestedParser)
}

fun interface AccumulatingContentParser {
    fun parse(input: String, accumulator: ContentAccumulator, nestingLevel: Int): ContentAccumulator
}

class RichMessageParser(
    private val accumulatingParser: AccumulatingContentParser = AccumulatingRichTextContentParser()
) {

    fun parse(source: String): RichText {
        val input = source
            .removeHtmlEntities()
            .dropTextFallback()
        return RichText(accumulatingParser.parse(input, RichTextPartBuilder(), nestingLevel = 0).build())
    }

}

private fun String.removeHtmlEntities() = this.replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")

private fun String.dropTextFallback() = this.lines()
    .dropWhile { it.startsWith("> ") || it.isEmpty() }
    .joinToString(separator = "\n")
