package app.dapk.st.matrix.common.extensions

import app.dapk.st.matrix.common.JsonString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

fun Any?.toJsonString(): JsonString = JsonString(Json.encodeToString(this.toJsonElement()))

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
    else -> throw IllegalArgumentException("Unknown type: $this")
}
