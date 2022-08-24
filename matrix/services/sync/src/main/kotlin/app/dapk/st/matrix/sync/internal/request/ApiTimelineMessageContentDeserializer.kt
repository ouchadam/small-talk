package app.dapk.st.matrix.sync.internal.request

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object ApiTimelineMessageContentDeserializer : KSerializer<ApiTimelineEvent.TimelineMessage.Content> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("messageContent")

    override fun deserialize(decoder: Decoder): ApiTimelineEvent.TimelineMessage.Content {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (element.jsonObject["msgtype"]?.jsonPrimitive?.content) {
            "m.text" -> decoder.json.decodeFromJsonElement(ApiTimelineEvent.TimelineMessage.Content.Text.serializer(), element)
            "m.image" -> decoder.json.decodeFromJsonElement(ApiTimelineEvent.TimelineMessage.Content.Image.serializer(), element)
            else -> ApiTimelineEvent.TimelineMessage.Content.Ignored
        }
    }

    override fun serialize(encoder: Encoder, value: ApiTimelineEvent.TimelineMessage.Content) = when (value) {
        ApiTimelineEvent.TimelineMessage.Content.Ignored -> {}
        is ApiTimelineEvent.TimelineMessage.Content.Image -> ApiTimelineEvent.TimelineMessage.Content.Image.serializer().serialize(encoder, value)
        is ApiTimelineEvent.TimelineMessage.Content.Text -> ApiTimelineEvent.TimelineMessage.Content.Text.serializer().serialize(encoder, value)
    }

}