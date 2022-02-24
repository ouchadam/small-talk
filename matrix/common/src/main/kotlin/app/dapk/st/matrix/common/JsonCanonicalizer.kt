package app.dapk.st.matrix.common

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private val CAPTURE_UNICODE = "\\\\u(.{4})".toRegex()

class JsonCanonicalizer {

    fun canonicalize(input: JsonString): String {
        val element = Json.parseToJsonElement(input.value.replace(CAPTURE_UNICODE, " ")).sort()
        return Json.encodeToString(element)
    }

}

private fun JsonElement.sort(): JsonElement {
    return when (this) {
        is JsonObject -> JsonObject(
            this.map { it.key to it.value.sort() }.sortedBy { it.first }.toMap()
        )
        is JsonArray -> JsonArray(this.map { it.sort() })
        else -> this
    }
}