package app.dapk.st.matrix.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RichText(@SerialName("parts") val parts: List<Part>) {
    @Serializable
    sealed interface Part {
        @Serializable
        data class Normal(@SerialName("content") val content: String) : Part

        @Serializable
        data class Link(@SerialName("url") val url: String, @SerialName("label") val label: String) : Part

        @Serializable
        data class Bold(@SerialName("content") val content: String) : Part

        @Serializable
        data class Italic(@SerialName("content") val content: String) : Part

        @Serializable
        data class BoldItalic(@SerialName("content") val content: String) : Part

        @Serializable
        data class Person(@SerialName("user_id") val userId: UserId, @SerialName("display_name") val displayName: String) : Part
    }

    companion object {
        fun of(text: String) = RichText(listOf(RichText.Part.Normal(text)))
    }
}

fun RichText.asString() = parts.joinToString(separator = "") {
    when(it) {
        is RichText.Part.Bold -> it.content
        is RichText.Part.BoldItalic -> it.content
        is RichText.Part.Italic -> it.content
        is RichText.Part.Link -> it.label
        is RichText.Part.Normal -> it.content
        is RichText.Part.Person -> it.userId.value
    }
}