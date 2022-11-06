package app.dapk.st.matrix.sync.internal.sync.message.html

import app.dapk.st.matrix.sync.internal.sync.message.AccumulatingContentParser
import app.dapk.st.matrix.sync.internal.sync.message.ContentAccumulator

class HtmlProcessor {

    private val tagCaptor = TagCaptor()
    private val htmlTagParser = RichTextHtmlTagParser()

    fun process(input: String, tagOpen: Int, partBuilder: ContentAccumulator, nestingLevel: Int, nestedParser: AccumulatingContentParser): Int {
        val afterTagCaptureIndex = tagCaptor.tagCapture(input, tagOpen) { tagName, attributes, tagContent ->
            htmlTagParser.parse(tagName, attributes, tagContent, partBuilder) { nestedContent, accumulator ->
                nestedParser.parse(nestedContent, accumulator, nestingLevel + 1)
            }
        }
        return when (afterTagCaptureIndex) {
            -1 -> {
                partBuilder.appendText(input[tagOpen].toString())
                tagOpen + 1
            }

            else -> afterTagCaptureIndex
        }
    }

}