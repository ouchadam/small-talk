package app.dapk.st.matrix.sync.internal.sync.message.html

class TagCaptor {

    fun tagCapture(input: String, startIndex: Int, tagFactory: (String, Map<String, String>, String) -> Unit): Int {
        return when (val closeIndex = input.indexOf('>', startIndex = startIndex)) {
            -1 -> -1
            else -> {
                val fullTag = input.substring(startIndex, closeIndex + 1)
                val tagName = input.substring(startIndex + 1, closeIndex)
                when {
                    fullTag.isExitlessTag() -> {
                        val trim = fullTag.removeSurrounding("<", ">").trim()
                        tagFactory(trim, emptyMap(), "")
                        closeIndex + 1
                    }

                    fullTag.isSelfClosing() -> {
                        val trim = fullTag.removeSuffix("/>").removePrefix("<").trim()
                        tagFactory(trim, emptyMap(), "")
                        closeIndex + 1
                    }

                    else -> {
                        val exitTag = if (tagName.contains(' ')) {
                            "</${tagName.substringBefore(' ')}>"
                        } else {
                            "</$tagName>"
                        }

                        val exitIndex = input.findTagClose(tagName, exitTag, searchIndex = closeIndex + 1)
                        if (exitIndex == -1) {
                            -1
                        } else {
                            val exitTagCloseIndex = exitIndex + exitTag.length
                            if (tagName.contains(' ')) {
                                val parts = tagName.split(' ')
                                val attributes = parts.drop(1).associate {
                                    val (key, value) = it.split("=")
                                    key to value.removeSurrounding("\"")
                                }
                                tagFactory(parts.first(), attributes, input.substring(closeIndex + 1, exitIndex))
                            } else {
                                tagFactory(tagName, emptyMap(), input.substring(closeIndex + 1, exitIndex))
                            }
                            exitTagCloseIndex
                        }
                    }
                }
            }
        }
    }

    private fun String.findTagClose(tagName: String, exitTag: String, searchIndex: Int, open: Int = 1): Int {
        val exitIndex = this.indexOf(exitTag, startIndex = searchIndex)
        val nextOpen = this.indexOf("<$tagName", startIndex = searchIndex)
        return when {
            open == 1 && (nextOpen == -1 || exitIndex < nextOpen) -> exitIndex
            open > 8 || open < 1 -> {
                // something has gone wrong, lets exit
                -1
            }

            exitIndex == -1 -> -1
            nextOpen == -1 || nextOpen > exitIndex -> this.findTagClose(tagName, exitTag, exitIndex + 1, open - 1)

            nextOpen < exitIndex -> {
                this.findTagClose(tagName, exitTag, nextOpen + 1, open + 1)
            }

            else -> -1
        }
    }
}

private fun String.isExitlessTag() = this == "<br>" || (this.startsWith("<@") && this.endsWith('>'))

private fun String.isSelfClosing() = this.endsWith("/>")
