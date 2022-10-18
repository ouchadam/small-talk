package app.dapk.st.design.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

data class Event(val authorName: String, val edited: Boolean, val time: String)
data class ImageContent(val width: Int?, val height: Int?, val url: String)

@Composable
fun Bubble(bubble: BubbleMeta, content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(bubble.shape)
                .background(bubble.background)
                .height(IntrinsicSize.Max),
        ) {
            content()
        }
    }
}

@Composable
fun TextBubble(bubble: BubbleMeta, event: Event, textContent: String, status: @Composable () -> Unit) {
    Bubble(bubble) {
        Column(
            Modifier
                .padding(8.dp)
                .width(IntrinsicSize.Max)
                .defaultMinSize(minWidth = 50.dp)
        ) {
            if (bubble.isNotSelf()) {
                AuthorName(event, bubble)
            }
            TextContent(bubble, text = textContent)
            Footer(event, bubble, status)
        }
    }
}

@Composable
fun EncryptedBubble(bubble: BubbleMeta, event: Event, status: @Composable () -> Unit) {
    TextBubble(bubble, event, textContent = "Encrypted message", status)
}

@Composable
fun ImageBubble(bubble: BubbleMeta, event: Event, imageContent: ImageContent, status: @Composable () -> Unit, imageRequest: ImageRequest) {
    Bubble(bubble) {
        Column(
            Modifier
                .padding(8.dp)
                .width(IntrinsicSize.Max)
                .defaultMinSize(minWidth = 50.dp)
        ) {
            if (bubble.isNotSelf()) {
                AuthorName(event, bubble)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Image(
                modifier = Modifier.size(imageContent.scale(LocalDensity.current, LocalConfiguration.current)),
                painter = rememberAsyncImagePainter(model = imageRequest),
                contentDescription = null,
            )
            Footer(event, bubble, status)
        }
    }
}

private fun ImageContent.scale(density: Density, configuration: Configuration): DpSize {
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


@Composable
private fun Footer(event: Event, bubble: BubbleMeta, status: @Composable () -> Unit) {
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
private fun AuthorName(event: Event, bubble: BubbleMeta) {
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