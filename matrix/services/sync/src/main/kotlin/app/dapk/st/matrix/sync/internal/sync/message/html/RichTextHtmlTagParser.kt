package app.dapk.st.matrix.sync.internal.sync.message.html

import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.internal.sync.message.*

class RichTextHtmlTagParser : TagParser {

    override fun parse(
        tagName: String,
        attributes: Map<String, String>,
        content: String,
        accumulator: ContentAccumulator,
        parser: NestedParser
    ) {
        when {
            tagName.startsWith('@') -> {
                accumulator.appendPerson(UserId(tagName), tagName)
            }

            else -> when (tagName) {
                "br" -> {
                    accumulator.appendNewline()
                }

                "a" -> {
                    attributes["href"]?.let { url ->
                        when {
                            url.startsWith("https://matrix.to/#/@") -> {
                                val userId = UserId(url.substringAfter("https://matrix.to/#/").substringBeforeLast("\""))
                                accumulator.appendPerson(userId, "@${content.removePrefix("@")}")
                            }

                            else -> accumulator.appendLink(url, content)

                        }
                    } ?: accumulator.appendText(content)
                }

                "p" -> {
                    parser.parse(content.trim(), accumulator)
                    accumulator.appendNewline()
                }

                "blockquote" -> {
                    accumulator.appendText("> ")
                    parser.parse(content.trim(), accumulator)
                }

                "strong", "b" -> {
                    accumulator.appendBold(content)
                }

                "em", "i" -> {
                    accumulator.appendItalic(content)
                }

                "h1", "h2", "h3", "h4", "h5" -> {
                    accumulator.appendBold(content)
                    accumulator.appendNewline()
                }

                "ul", "ol" -> {
                    when (tagName) {
                        "ol" -> parser.parse(content, OrderedListAccumulator(accumulator))
                        "ul" -> parser.parse(content, UnorderedListAccumulator(accumulator))
                    }
                }

                "li" -> {
                    (accumulator as ListAccumulator).appendLinePrefix(attributes["value"]?.toInt())

                    val nestedList = when {
                        content.contains("<ul>") -> "<ul>"
                        content.contains("<ol>") -> "<ol>"
                        else -> null
                    }

                    if (nestedList == null) {
                        parser.parse(content.trim(), accumulator)
                        accumulator.appendNewline()
                    } else {
                        val firstItemInNested = content.substringBefore(nestedList)
                        parser.parse(firstItemInNested.trim(), accumulator)
                        accumulator.appendNewline()
                        parser.parse(content.substring(content.indexOf(nestedList)).trim(), accumulator)
                    }
                }

                else -> {
                    // skip tag
                }
            }
        }
    }
}