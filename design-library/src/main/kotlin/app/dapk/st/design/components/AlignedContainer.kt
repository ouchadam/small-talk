package app.dapk.st.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val selfBackgroundShape = RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
private val othersBackgroundShape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)

data class BubbleMeta(
    val shape: RoundedCornerShape,
    val background: Color,
    val isSelf: Boolean,
)

fun BubbleMeta.isNotSelf() = !this.isSelf

@Composable
fun LazyItemScope.AlignedDraggableContainer(
    avatar: Avatar,
    isSelf: Boolean,
    wasPreviousMessageSameSender: Boolean,
    onReply: () -> Unit,
    content: @Composable BubbleMeta.() -> Unit
) {
    val rowWithMeta = @Composable {
        DraggableRow(
            avatar = avatar,
            isSelf = isSelf,
            wasPreviousMessageSameSender = wasPreviousMessageSameSender,
            onReply = { onReply() }
        ) {
            content(
                when (isSelf) {
                    true -> BubbleMeta(selfBackgroundShape, SmallTalkTheme.extendedColors.selfBubble, isSelf = true)
                    false -> BubbleMeta(othersBackgroundShape, SmallTalkTheme.extendedColors.othersBubble, isSelf = false)
                }
            )
        }
    }

    when (isSelf) {
        true -> SelfContainer(rowWithMeta)
        false -> OtherContainer(rowWithMeta)
    }
}

@Composable
private fun LazyItemScope.OtherContainer(content: @Composable () -> Unit) {
    Box(modifier = Modifier.Companion.fillParentMaxWidth(0.95f), contentAlignment = Alignment.TopStart) {
        content()
    }
}

@Composable
private fun LazyItemScope.SelfContainer(content: @Composable () -> Unit) {
    Box(modifier = Modifier.Companion.fillParentMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Box(modifier = Modifier.Companion.fillParentMaxWidth(0.85f), contentAlignment = Alignment.TopEnd) {
            content()
        }
    }
}

@Composable
private fun DraggableRow(
    isSelf: Boolean,
    wasPreviousMessageSameSender: Boolean,
    onReply: () -> Unit,
    avatar: Avatar,
    content: @Composable () -> Unit
) {

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val localDensity = LocalDensity.current

    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    Row(
        Modifier.padding(horizontal = 12.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState {
                    if ((offsetX.value + it) > 0) {
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + it) }
                    }
                },
                onDragStopped = {
                    with(localDensity) {
                        if (offsetX.value > (screenWidthDp.toPx() * 0.15)) {
                            onReply()
                        }
                    }

                    coroutineScope.launch {
                        offsetX.animateTo(targetValue = 0f)
                    }
                }
            )
    ) {
        when (isSelf) {
            true -> {
                // do nothing
            }

            false -> SenderAvatar(wasPreviousMessageSameSender, avatar)
        }
        content()
    }
}

@Composable
private fun SenderAvatar(wasPreviousMessageSameSender: Boolean, avatar: Avatar) {
    val displayImageSize = 32.dp
    when {
        wasPreviousMessageSameSender -> {
            Spacer(modifier = Modifier.width(displayImageSize))
        }

        avatar.url == null -> {
            MissingAvatarIcon(avatar.name, displayImageSize)
        }

        else -> {
            MessengerUrlIcon(avatar.url, displayImageSize)
        }
    }
}

data class Avatar(val url: String?, val name: String)