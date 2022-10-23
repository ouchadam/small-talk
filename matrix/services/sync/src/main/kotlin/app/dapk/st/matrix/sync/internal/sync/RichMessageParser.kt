package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText

data class Tag(val name: String, val inner: String, val content: String)

class RichMessageParser {

    fun parse(input: String): RichText {
        val buffer = mutableSetOf<RichText.Part>()
        var openIndex = 0
        var closeIndex = 0
        while (openIndex != -1) {
            val foundIndex = input.indexOf('<', startIndex = openIndex)
            if (foundIndex != -1) {
                closeIndex = input.indexOf('>', startIndex = openIndex)

                if (closeIndex == -1) {
                    openIndex++
                } else {
                    val wholeTag = input.substring(foundIndex, closeIndex + 1)
                    val tagName = wholeTag.substring(1, wholeTag.indexOfFirst { it == '>' || it == ' ' })
                    val exitTag = "<$tagName/>"
                    val exitIndex = input.indexOf(exitTag, startIndex = closeIndex + 1)
                    val tagContent = input.substring(closeIndex + 1, exitIndex)

                    val tag = Tag(name = tagName, wholeTag, tagContent)

                    println("found $tag")
                    if (openIndex != foundIndex) {
                        buffer.add(RichText.Part.Normal(input.substring(openIndex, foundIndex)))
                    }
                    openIndex = exitIndex + exitTag.length

                    when (tagName) {
                        "a" -> {
                            val findHrefUrl = wholeTag.substringAfter("href=").replace("\"", "").removeSuffix(">")
                            buffer.add(RichText.Part.Link(url = findHrefUrl, label = tag.content))
                        }

                        "b" -> buffer.add(RichText.Part.Bold(tagContent))
                        "i" -> buffer.add(RichText.Part.Italic(tagContent))
                    }
                }
            } else {
                // exit
                if (openIndex < input.length) {
                    buffer.add(RichText.Part.Normal(input.substring(openIndex)))
                }
                break
            }
        }

        return RichText(buffer)
    }

}
