package app.dapk.st.design.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

sealed interface BubbleModel {
    val event: Event

    data class Text(val content: String, override val event: Event) : BubbleModel
    data class Encrypted(override val event: Event) : BubbleModel
    data class Image(val imageContent: ImageContent, val imageRequest: ImageRequest, override val event: Event) : BubbleModel {
        data class ImageContent(val width: Int?, val height: Int?, val url: String)
    }

    data class Reply(val replyingTo: BubbleModel, val reply: BubbleModel) : BubbleModel {
        override val event = reply.event
    }

    data class Event(val authorId: String, val authorName: String, val edited: Boolean, val time: String)

}

private fun BubbleModel.Reply.isReplyingToSelf() = this.replyingTo.event.authorId == this.reply.event.authorId

@Composable
fun MessageBubble(bubble: BubbleMeta, model: BubbleModel, status: @Composable () -> Unit, onLongClick: (BubbleModel) -> Unit) {
    val itemisedLongClick = { onLongClick.invoke(model) }
    when (model) {
        is BubbleModel.Text -> TextBubble(bubble, model, status, itemisedLongClick)
        is BubbleModel.Encrypted -> EncryptedBubble(bubble, model, status, itemisedLongClick)
        is BubbleModel.Image -> ImageBubble(bubble, model, status, itemisedLongClick)
        is BubbleModel.Reply -> ReplyBubble(bubble, model, status, itemisedLongClick)
    }
}

@Composable
private fun TextBubble(bubble: BubbleMeta, model: BubbleModel.Text, status: @Composable () -> Unit, onLongClick: () -> Unit) {
    Bubble(bubble, onLongClick) {
        if (bubble.isNotSelf()) {
            AuthorName(model.event, bubble)
        }
        TextContent(bubble, text = model.content)
        Footer(model.event, bubble, status)
    }
}

@Composable
private fun EncryptedBubble(bubble: BubbleMeta, model: BubbleModel.Encrypted, status: @Composable () -> Unit, onLongClick: () -> Unit) {
    TextBubble(bubble, BubbleModel.Text(content = "Encrypted message", model.event), status, onLongClick)
}

@Composable
private fun ImageBubble(bubble: BubbleMeta, model: BubbleModel.Image, status: @Composable () -> Unit, onLongClick: () -> Unit) {
    Bubble(bubble, onLongClick) {
        if (bubble.isNotSelf()) {
            AuthorName(model.event, bubble)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Image(
            modifier = Modifier.size(model.imageContent.scale(LocalDensity.current, LocalConfiguration.current)),
            painter = rememberAsyncImagePainter(model = model.imageRequest),
            contentDescription = null,
        )
        Footer(model.event, bubble, status)
    }
}

@Composable
private fun ReplyBubble(bubble: BubbleMeta, model: BubbleModel.Reply, status: @Composable () -> Unit, onLongClick: () -> Unit) {
    Bubble(bubble, onLongClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    if (bubble.isNotSelf()) SmallTalkTheme.extendedColors.onOthersBubble.copy(alpha = 0.1f) else SmallTalkTheme.extendedColors.onSelfBubble.copy(
                        alpha = 0.2f
                    ), RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            val replyName = if (!bubble.isNotSelf() && model.isReplyingToSelf()) "You" else model.replyingTo.event.authorName
            Text(
                fontSize = 11.sp,
                text = replyName,
                maxLines = 1,
                color = bubble.textColor()
            )
            Spacer(modifier = Modifier.height(2.dp))

            when (val replyingTo = model.replyingTo) {
                is BubbleModel.Text -> {
                    Text(
                        text = replyingTo.content,
                        color = bubble.textColor().copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.wrapContentSize(),
                        textAlign = TextAlign.Start,
                    )
                }

                is BubbleModel.Encrypted -> {
                    Text(
                        text = "Encrypted message",
                        color = bubble.textColor().copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.wrapContentSize(),
                        textAlign = TextAlign.Start,
                    )
                }

                is BubbleModel.Image -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        modifier = Modifier.size(replyingTo.imageContent.scale(LocalDensity.current, LocalConfiguration.current)),
                        painter = rememberAsyncImagePainter(replyingTo.imageRequest),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                is BubbleModel.Reply -> {
                    // TODO - a reply to a reply
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (bubble.isNotSelf()) {
            AuthorName(model.event, bubble)
        }

        when (val message = model.reply) {
            is BubbleModel.Text -> TextContent(bubble, message.content)
            is BubbleModel.Encrypted -> TextContent(bubble, "Encrypted message")
            is BubbleModel.Image -> {
                Spacer(modifier = Modifier.height(4.dp))
                Image(
                    modifier = Modifier.size(message.imageContent.scale(LocalDensity.current, LocalConfiguration.current)),
                    painter = rememberAsyncImagePainter(model = message.imageRequest),
                    contentDescription = null,
                )
            }

            is BubbleModel.Reply -> {
                // TODO - a reply to a reply
            }
        }

        Footer(model.event, bubble, status)
    }
}

private fun BubbleModel.Image.ImageContent.scale(density: Density, configuration: Configuration): DpSize {
    val height = this@scale.height ?: 250
    val width = this@scale.width ?: 250
    return with(density) {
        val scaler = minOf(
            height.scalerFor(configuration.screenHeightDp.dp.toPx() * 0.5f),
            width.scalerFor(configuration.screenWidthDp.dp.toPx() * 0.6f)
        )

        DpSize(
            width = (width * scaler).toDp(),
            height = (height * scaler).toDp(),
        )
    }
}

private fun Int.scalerFor(max: Float): Float {
    return max / this
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(bubble: BubbleMeta, onLongClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(bubble.shape)
                .background(bubble.background)
                .height(IntrinsicSize.Max)
                .combinedClickable(onLongClick = onLongClick, onClick = {}),
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 50.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun Footer(event: BubbleModel.Event, bubble: BubbleMeta, status: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        val editedPrefix = if (event.edited) "(edited) " else null
        Text(
            fontSize = 9.sp,
            text = "${editedPrefix ?: ""}${event.time}",
            textAlign = TextAlign.End,
            color = bubble.textColor(),
            modifier = Modifier.wrapContentSize()
        )
        status()
    }
}

@Composable
private fun TextContent(bubble: BubbleMeta, text: String) {
    Text(
        text = text,
        color = bubble.textColor(),
        fontSize = 15.sp,
        modifier = Modifier.wrapContentSize(),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun AuthorName(event: BubbleModel.Event, bubble: BubbleMeta) {
    Text(
        fontSize = 11.sp,
        text = event.authorName,
        maxLines = 1,
        color = bubble.textColor()
    )
}

@Composable
private fun BubbleMeta.textColor(): Color {
    return if (this.isSelf) SmallTalkTheme.extendedColors.onSelfBubble else SmallTalkTheme.extendedColors.onOthersBubble
}