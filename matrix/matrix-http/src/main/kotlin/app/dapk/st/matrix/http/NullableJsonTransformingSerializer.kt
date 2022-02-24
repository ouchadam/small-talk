package app.dapk.st.matrix.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

abstract class NullableJsonTransformingSerializer<T : Any>(
    private val tSerializer: KSerializer<T>,
    private val deserializer: (JsonElement) -> JsonElement?
) : KSerializer<T?> {

    override val descriptor: SerialDescriptor get() = tSerializer.descriptor

    final override fun deserialize(decoder: Decoder): T? {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return deserializer(element)?.let { decoder.json.decodeFromJsonElement(tSerializer, it) }
    }

    final override fun serialize(encoder: Encoder, value: T?) {
        throw IllegalAccessError("serialize not supported")
    }
}