package app.dapk.st.messenger

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

@Composable
fun MessengerScreen(roomId: RoomId, viewModel: MessengerViewModel, navigator: Navigator) {
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
            when (item) {
                is Message -> {
                    val wasPreviousMessageSameSender = when (val previousEvent = if (index != 0) state.events[index - 1] else null) {
                        null -> false
                        is Message -> previousEvent.author.id == item.author.id
                        is RoomEvent.Reply -> previousEvent.message.author.id == item.author.id
                    }
                    Message(self, item, wasPreviousMessageSameSender)
                }
                is RoomEvent.Reply -> {
                    val wasPreviousMessageSameSender = when (val previousEvent = if (index != 0) state.events[index - 1] else null) {
                        null -> false
                        is Message -> previousEvent.author.id == item.message.author.id
                        is RoomEvent.Reply -> previousEvent.message.author.id == item.message.author.id
                    }
                    Reply(self, item, wasPreviousMessageSameSender)
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.Message(self: UserId, message: Message, wasPreviousMessageSameSender: Boolean) {
    when (message.author.id == self) {
        true -> {
            Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillParentMaxWidth(0.85f), contentAlignment = Alignment.TopEnd) {
                    Bubble(
                        message = message,
                        isNotSelf = false,
                        wasPreviousMessageSameSender = wasPreviousMessageSameSender
                    ) {
                        TextBubbleContent(selfBackgroundShape, SmallTalkTheme.extendedColors.selfBubble, false, message)
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
                    TextBubbleContent(othersBackgroundShape, SmallTalkTheme.extendedColors.othersBubble, true, message)
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.Reply(self: UserId, message: RoomEvent.Reply, wasPreviousMessageSameSender: Boolean) {
    when (message.message.author.id == self) {
        true -> {
            Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillParentMaxWidth(0.85f), contentAlignment = Alignment.TopEnd) {
                    Bubble(
                        message = message.message,
                        isNotSelf = false,
                        wasPreviousMessageSameSender = wasPreviousMessageSameSender
                    ) {
                        ReplyBubbleContent(selfBackgroundShape, SmallTalkTheme.extendedColors.selfBubble, false, message)
                    }
                }
            }
        }
        false -> {
            Box(modifier = Modifier.fillParentMaxWidth(0.95f), contentAlignment = Alignment.TopStart) {
                Bubble(
                    message = message.message,
                    isNotSelf = true,
                    wasPreviousMessageSameSender = wasPreviousMessageSameSender
                ) {
                    ReplyBubbleContent(othersBackgroundShape, SmallTalkTheme.extendedColors.othersBubble, true, message)
                }
            }
        }
    }
}


private val selfBackgroundShape = RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
private val othersBackgroundShape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)

@Composable
private fun Bubble(
    message: Message,
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
private fun TextBubbleContent(shape: RoundedCornerShape, background: Color, isNotSelf: Boolean, message: Message) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(shape)
                .background(background)
                .height(IntrinsicSize.Max),
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .defaultMinSize(minWidth = 50.dp)
            ) {
                if (isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = message.author.displayName ?: message.author.id.value,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                Text(
                    text = message.content,
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.wrapContentSize(),
                    textAlign = TextAlign.Start,
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val editedPrefix = if (message.edited) "(edited) " else null
                    Text(
                        fontSize = 9.sp,
                        text = "${editedPrefix ?: ""}${message.time}",
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.wrapContentSize()
                    )
                    SendStatus(message)
                }
            }
        }
    }
}

@Composable
private fun ReplyBubbleContent(shape: RoundedCornerShape, background: Color, isNotSelf: Boolean, reply: RoomEvent.Reply) {
    Box(modifier = Modifier.padding(start = 6.dp)) {
        Box(
            Modifier
                .padding(4.dp)
                .clip(shape)
                .background(background)
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
                        .background(if (isNotSelf) SmallTalkTheme.extendedColors.otherBubbleReplyBackground else SmallTalkTheme.extendedColors.selfBubbleReplyBackground)
                        .padding(4.dp)
                ) {
                    val replyName = if (!isNotSelf && reply.replyingToSelf) "You" else reply.replyingTo.author.displayName ?: reply.replyingTo.author.id.value
                    Text(
                        fontSize = 11.sp,
                        text = replyName,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                    Text(
                        text = reply.replyingTo.content,
                        color = MaterialTheme.colors.onPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.wrapContentSize(),
                        textAlign = TextAlign.Start,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = reply.message.author.displayName ?: reply.message.author.id.value,
                        maxLines = 1,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                Text(
                    text = reply.message.content,
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.wrapContentSize(),
                    textAlign = TextAlign.Start,
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        fontSize = 9.sp,
                        text = reply.time,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.wrapContentSize()
                    )
                    SendStatus(reply.message)
                }
            }
        }
    }
}


@Composable
private fun RowScope.SendStatus(message: Message) {
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
