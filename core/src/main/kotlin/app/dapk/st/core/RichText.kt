package app.dapk.st.core

data class RichText(val parts: Set<Part>) {
    sealed interface Part {
        data class Normal(val content: String) : Part
        data class Link(val url: String, val label: String) : Part
        data class Bold(val content: String) : Part
        data class Italic(val content: String) : Part
        data class BoldItalic(val content: String) : Part
    }
}

fun RichText.asString() = parts.joinToString(separator = "")