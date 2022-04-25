package app.dapk.st.messenger

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import app.dapk.st.core.Lce
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.*
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomEvent.Message
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.navigator.Navigator
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch

@Composable
internal fun MessengerScreen(roomId: RoomId, viewModel: MessengerViewModel, navigator: Navigator) {
    val state = viewModel.state

    viewModel.ObserveEvents()
    LifecycleEffect(
        onStart = { viewModel.post(MessengerAction.OnMessengerVisible(roomId)) },
        onStop = { viewModel.post(MessengerAction.OnMessengerGone) }
    )

    val roomTitle = when (val roomState = state.roomState) {
        is Lce.Content -> roomState.value.roomState.roomOverview.roomName
        else -> null
    }

    Column {
        Toolbar(onNavigate = { navigator.navigate.upToHome() }, roomTitle, actions = {
            OverflowMenu {
                DropdownMenuItem(onClick = {}) {
                    Text("Settings")
                }
            }
        })
        Room(state.roomState)
        when (state.composerState) {
            is ComposerState.Text -> {
                Composer(
                    state.composerState.value,
                    onTextChange = { viewModel.post(MessengerAction.ComposerTextUpdate(it)) },
                    onSend = { viewModel.post(MessengerAction.ComposerSendText) },
                )
            }
        }
    }
}

@Composable
private fun MessengerViewModel.ObserveEvents() {
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
            }
        }
    }
}

@Composable
private fun ColumnScope.Room(roomStateLce: Lce<MessengerState>) {
    when (val state = roomStateLce) {
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            RoomContent(state.value.self, state.value.roomState)
            val eventBarHeight = 14.dp
            val typing = state.value.typing
            when {
                typing == null || typing.members.isEmpty() -> Spacer(modifier = Modifier.height(eventBarHeight))
                else -> {
                    Box(
                        modifier = Modifier
                            .height(eventBarHeight)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp), contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            fontSize = 10.sp,
                            text = if (typing.members.size > 1) {
                                "People are typing..."
                            } else {
                                val member = typing.members.first()
                                val name = member.displayName ?: member.id.value
                                "$name is typing..."
                            },
                            maxLines = 1,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
        is Lce.Error -> {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Something went wrong...")
                    Button(onClick = {}) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RoomContent(self: UserId, state: RoomState) {
    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = state.events.size) {
        if (listState.firstVisibleItemScrollOffset <= 1) {
            scope.launch { listState.scrollToItem(0) }
        } else {
            // TODO show has new messages
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(vertical = 8.dp),
        state = listState, reverseLayout = true
    ) {
        itemsIndexed(
            items = state.events,
            key = { _, item -> item.eventId.value },
        ) { index, item ->
            val previousEvent = if (index != 0) state.events[index - 1] else null
            val wasPreviousMessageSameSender = previousEvent?.author?.id == item.author.id
            AlignedBubble(item, self, wasPreviousMessageSameSender) {
                when (item) {
                    is RoomEvent.Image -> MessageImage(it as BubbleContent<RoomEvent.Image>)
                    is Message -> TextBubbleContent(it as BubbleContent<RoomEvent.Message>)
                    is RoomEvent.Reply -> ReplyBubbleContent(it as BubbleContent<RoomEvent.Reply>)
                }
            }
        }
    }
}

private data class BubbleContent<T : RoomEvent>(
    val shape: RoundedCornerShape,
    val background: Color,
    val isNotSelf: Boolean,
    val message: T
)

@Composable
private fun <T : RoomEvent> LazyItemScope.AlignedBubble(
    message: T,
    self: UserId,
    wasPreviousMessageSameSender: Boolean,
    content: @Composable (BubbleContent<T>) -> Unit
) {
    when (message.author.id == self) {
        true -> {
            Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillParentMaxWidth(0.85f), contentAlignment = Alignment.TopEnd) {
                    Bubble(
                        message = message,
                        isNotSelf = false,
                        wasPreviousMessageSameSender = wasPreviousMessageSameSender
                    ) {
                        content(BubbleContent(selfBackgroundShape, SmallTalkTheme.extendedColors.selfBubble, false, message))
                    }
                }
            }
        }
        false -> {
            Box(modifier = Modifier.fillParentMaxWidth(0.95f), contentAlignment = Alignment.TopStart) {
                Bubble(
                    message = message,
                    isNotSelf = true,
                    wasPreviousMessageSameSender = wasPreviousMessageSameSender
                ) {
                    content(BubbleContent(othersBackgroundShape, SmallTalkTheme.extendedColors.othersBubble, true, message))
                }
            }
        }
    }
}

private val decryptingFetcher = DecryptingFetcher()

@Composable
private fun MessageImage(content: BubbleContent<RoomEvent.Image>) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(content.shape)
                .background(content.background)
                .height(IntrinsicSize.Max),
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 50.dp)
            ) {
                if (content.isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = content.message.author.displayName ?: content.message.author.id.value,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Image(
                    modifier = Modifier.size(content.message.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                    painter = rememberImagePainter(
                        data = content.message,
                        builder = { fetcher(decryptingFetcher) }
                    ),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val editedPrefix = if (content.message.edited) "(edited) " else null
                    Text(
                        fontSize = 9.sp,
                        text = "${editedPrefix ?: ""}${content.message.time}",
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.wrapContentSize()
                    )
                    SendStatus(content.message)
                }
            }
        }
    }
}

private fun RoomEvent.Image.ImageMeta.scale(density: Density, configuration: Configuration): DpSize {
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

private val selfBackgroundShape = RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
private val othersBackgroundShape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)

@Composable
private fun Bubble(
    message: RoomEvent,
    isNotSelf: Boolean,
    wasPreviousMessageSameSender: Boolean,
    content: @Composable () -> Unit
) {
    Row(Modifier.padding(horizontal = 12.dp)) {
        when {
            isNotSelf -> {
                val displayImageSize = 32.dp
                when {
                    wasPreviousMessageSameSender -> {
                        Spacer(modifier = Modifier.width(displayImageSize))
                    }
                    message.author.avatarUrl == null -> {
                        MissingAvatarIcon(message.author.displayName ?: message.author.id.value, displayImageSize)
                    }
                    else -> {
                        MessengerUrlIcon(message.author.avatarUrl!!.value, displayImageSize)
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun TextBubbleContent(content: BubbleContent<RoomEvent.Message>) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(content.shape)
                .background(content.background)
                .height(IntrinsicSize.Max),
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 50.dp)
            ) {
                if (content.isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = content.message.author.displayName ?: content.message.author.id.value,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                Text(
                    text = content.message.content,
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.wrapContentSize(),
                    textAlign = TextAlign.Start,
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val editedPrefix = if (content.message.edited) "(edited) " else null
                    Text(
                        fontSize = 9.sp,
                        text = "${editedPrefix ?: ""}${content.message.time}",
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.wrapContentSize()
                    )
                    SendStatus(content.message)
                }
            }
        }
    }
}

@Composable
private fun ReplyBubbleContent(content: BubbleContent<RoomEvent.Reply>) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(content.shape)
                .background(content.background)
                .height(IntrinsicSize.Max),
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 50.dp)
            ) {
                Column(
                    Modifier
                        .background(if (content.isNotSelf) SmallTalkTheme.extendedColors.otherBubbleReplyBackground else SmallTalkTheme.extendedColors.selfBubbleReplyBackground)
                        .padding(4.dp)
                ) {
                    val replyName = if (!content.isNotSelf && content.message.replyingToSelf) "You" else content.message.replyingTo.author.displayName
                        ?: content.message.replyingTo.author.id.value
                    Text(
                        fontSize = 11.sp,
                        text = replyName,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                    when (val replyingTo = content.message.replyingTo) {
                        is Message -> {
                            Text(
                                text = replyingTo.content,
                                color = MaterialTheme.colors.onPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier.wrapContentSize(),
                                textAlign = TextAlign.Start,
                            )
                        }
                        is RoomEvent.Image -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Image(
                                modifier = Modifier.size(replyingTo.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                                painter = rememberImagePainter(
                                    data = replyingTo,
                                    builder = { fetcher(DecryptingFetcher()) }
                                ),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (content.isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = content.message.message.author.displayName ?: content.message.message.author.id.value,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                when (val message = content.message.message) {
                    is Message -> {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colors.onPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.wrapContentSize(),
                            textAlign = TextAlign.Start,
                        )
                    }
                    is RoomEvent.Image -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            modifier = Modifier.size(message.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                            painter = rememberImagePainter(
                                data = content.message,
                                builder = { fetcher(DecryptingFetcher()) }
                            ),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        fontSize = 9.sp,
                        text = content.message.time,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.wrapContentSize()
                    )
                    SendStatus(content.message.message)
                }
            }
        }
    }
}


@Composable
private fun RowScope.SendStatus(message: RoomEvent) {
    when (val meta = message.meta) {
        MessageMeta.FromServer -> {
            // last message is self
        }
        is MessageMeta.LocalEcho -> {
            when (val state = meta.state) {
                MessageMeta.LocalEcho.State.Sending, MessageMeta.LocalEcho.State.Sent -> {
                    val isSent = state == MessageMeta.LocalEcho.State.Sent
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, MaterialTheme.colors.onPrimary, CircleShape)
                            .size(10.dp)
                            .padding(2.dp)
                    ) {
                        if (isSent) {
                            Icon(imageVector = Icons.Filled.Check, "", tint = MaterialTheme.colors.onPrimary)
                        }
                    }
                }
                is MessageMeta.LocalEcho.State.Error -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, MaterialTheme.colors.error, CircleShape)
                            .size(10.dp)
                            .padding(2.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, "", tint = MaterialTheme.colors.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(message: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min), verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Bottom)
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(Modifier.padding(14.dp)) {
                if (message.isEmpty()) {
                    Text("Message")
                }
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = message,
                    onValueChange = { onTextChange(it) },
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current.copy(LocalContentAlpha.current))
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        var size by remember { mutableStateOf(IntSize(0, 0)) }
        IconButton(
            enabled = message.isNotEmpty(),
            modifier = Modifier
                .clip(CircleShape)
                .background(if (message.isEmpty()) Color.DarkGray else MaterialTheme.colors.primary)
                .run {
                    if (size.height == 0 || size.width == 0) {
                        this
                            .onSizeChanged {
                                size = it
                            }
                            .fillMaxHeight()
                    } else {
                        with(LocalDensity.current) {
                            size(size.width.toDp(), size.height.toDp())
                        }
                    }
                },
            onClick = onSend,
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "",
                tint = MaterialTheme.colors.onPrimary,
            )
        }
    }
}
