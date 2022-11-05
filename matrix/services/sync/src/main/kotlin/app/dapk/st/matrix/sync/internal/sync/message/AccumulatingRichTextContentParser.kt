package app.dapk.st.matrix.sync.internal.sync.message

import app.dapk.st.matrix.sync.internal.sync.message.html.HtmlProcessor
import app.dapk.st.matrix.sync.internal.sync.message.url.UrlParser

private const val MAX_NESTING_LIMIT = 20

class AccumulatingRichTextContentParser : AccumulatingContentParser {

    private val urlParser = UrlParser()
    private val tagProcessor = HtmlProcessor()

    override fun parse(input: String, accumulator: ContentAccumulator, nestingLevel: Int): ContentAccumulator {
        if (nestingLevel >= MAX_NESTING_LIMIT) {
            accumulator.appendText(input)
        } else {
            iterate { index ->
                process(
                    input,
                    index,
                    processTag = {
                        prependTextBeforeCapture(input, index, it, accumulator)
                        tagProcessor.process(input, it, accumulator, nestingLevel, nestedParser = this)
                    },
                    processUrl = {
                        prependTextBeforeCapture(input, index, it, accumulator)
                        urlParser.parseUrl(input, it, accumulator)
                    }
                ).also {
                    if (it == -1) {
                        appendRemainingText(index, input, accumulator)
                    }
                }
            }
        }
        return accumulator
    }

    private inline fun iterate(action: (Int) -> Int) {
        var result = 0
        while (result != -1) {
            result = action(result)
        }
    }

    private fun process(input: String, searchIndex: Int, processTag: (Int) -> Int, processUrl: (Int) -> Int): Int {
        val tagOpen = input.indexOf('<', startIndex = searchIndex)
        val httpOpen = input.indexOf("http", startIndex = searchIndex)
        return selectProcessor(
            tagOpen,
            httpOpen,
            processTag = { processTag(tagOpen) },
            processUrl = { processUrl(httpOpen) }
        )
    }

    private inline fun selectProcessor(tagOpen: Int, httpOpen: Int, processTag: () -> Int, processUrl: () -> Int) = when {
        tagOpen == -1 && httpOpen == -1 -> -1
        tagOpen != -1 && httpOpen == -1 -> processTag()
        tagOpen == -1 && httpOpen != -1 -> processUrl()
        tagOpen == httpOpen -> {
            // favour tags as urls can existing within tags
            processTag()
        }

        else -> {
            when (tagOpen < httpOpen) {
                true -> processTag()
                false -> processUrl()
            }
        }
    }

    private fun prependTextBeforeCapture(input: String, index: Int, captureIndex: Int, accumulator: ContentAccumulator) {
        if (index < captureIndex) {
            accumulator.appendText(input.substring(index, captureIndex))
        }
    }

    private fun appendRemainingText(index: Int, input: String, accumulator: ContentAccumulator) {
        if (index < input.length) {
            accumulator.appendText(input.substring(index, input.length))
        }
    }
}
