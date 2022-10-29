package app.dapk.st.core

data class RichText(val parts: List<Part>) {
    sealed interface Part {
        data class Normal(val content: String) : Part
        data class Link(val url: String, val label: String) : Part
        data class Bold(val content: String) : Part
        data class Italic(val content: String) : Part
        data class BoldItalic(val content: String) : Part
        data class Person(val displayName: String) : Part
    }
}

fun RichText.asString() = parts.joinToString(separator = "") {
    when(it) {
        is RichText.Part.Bold -> it.content
        is RichText.Part.BoldItalic -> it.content
        is RichText.Part.Italic -> it.content
        is RichText.Part.Link -> it.label
        is RichText.Part.Normal -> it.content
        is RichText.Part.Person -> it.displayName
    }
}