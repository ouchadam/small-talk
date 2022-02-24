package app.dapk.st.matrix.http

import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun <T> jsonBody(serializer: KSerializer<T>, payload: T, json: Json = MatrixHttpClient.json): OutgoingContent {
    return EqualTextContent(
        TextContent(
            text = json.encodeToString(serializer, payload),
            contentType = ContentType.Application.Json,
        )
    )
}

inline fun <reified T> jsonBody(payload: T, json: Json = MatrixHttpClient.json): OutgoingContent {
    return EqualTextContent(
        TextContent(
            text = json.encodeToString(payload),
            contentType = ContentType.Application.Json,
        )
    )
}

fun emptyJsonBody(): OutgoingContent {
    return EqualTextContent(TextContent("{}", ContentType.Application.Json))
}

class EqualTextContent(
    private val textContent: TextContent,
) : OutgoingContent.ByteArrayContent() {

    override fun bytes() = textContent.bytes()
    override val contentLength: Long
        get() = textContent.contentLength

    override fun toString(): String = textContent.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualTextContent
        if (!bytes().contentEquals(other.bytes())) return false
        return true
    }

    override fun hashCode() = bytes().hashCode()

}
