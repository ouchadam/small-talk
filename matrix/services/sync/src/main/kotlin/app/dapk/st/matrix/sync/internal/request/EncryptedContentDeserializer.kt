package app.dapk.st.matrix.sync.internal.request

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object EncryptedContentDeserializer : KSerializer<ApiEncryptedContent> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("encryptedContent")

    override fun deserialize(decoder: Decoder): ApiEncryptedContent {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (val algorithm = element.jsonObject["algorithm"]?.jsonPrimitive?.content) {
            "m.olm.v1.curve25519-aes-sha2" -> decoder.json.decodeFromJsonElement(ApiEncryptedContent.OlmV1.serializer(), element)
            "m.megolm.v1.aes-sha2" -> decoder.json.decodeFromJsonElement(ApiEncryptedContent.MegOlmV1.serializer(), element)
            null -> ApiEncryptedContent.Unknown
            else -> throw IllegalArgumentException("Unknown algorithm : $algorithm")
        }
    }

    override fun serialize(encoder: Encoder, value: ApiEncryptedContent) = TODO("Not yet implemented")

}