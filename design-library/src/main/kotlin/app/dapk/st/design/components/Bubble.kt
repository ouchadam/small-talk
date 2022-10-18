package app.dapk.st.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Event(val authorName: String, val edited: Boolean, val time: String)

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
fun TextBubbleContent(bubble: BubbleMeta, event: Event, textContent: String, status: @Composable () -> Unit) {
    Bubble(bubble) {
        Column(
            Modifier
                .padding(8.dp)
                .width(IntrinsicSize.Max)
                .defaultMinSize(minWidth = 50.dp)
        ) {
            if (bubble.isNotSelf()) {
                Text(
                    fontSize = 11.sp,
                    text = event.authorName,
                    maxLines = 1,
                    color = bubble.textColor()
                )
            }
            Text(
                text = textContent,
                color = bubble.textColor(),
                fontSize = 15.sp,
                modifier = Modifier.wrapContentSize(),
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun BubbleMeta.textColor(): Color {
    return if (this.isSelf) SmallTalkTheme.extendedColors.onSelfBubble else SmallTalkTheme.extendedColors.onOthersBubble
}