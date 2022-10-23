package app.dapk.st.matrix.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RichText(@SerialName("parts") val parts: Set<Part>) {
    @Serializable
    sealed interface Part {
        @Serializable
        data class Normal(@SerialName("content") val content: String) : Part
        data class Link(@SerialName("url") val url: String, @SerialName("label") val label: String) : Part
        data class Bold(@SerialName("content") val content: String) : Part
        data class Italic(@SerialName("content") val content: String) : Part
        data class BoldItalic(@SerialName("content") val content: String) : Part
    }
}

fun RichText.asString() = parts.joinToString(separator = "")