package app.dapk.st.messenger

import android.content.res.Configuration
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import app.dapk.st.core.Lce
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.core.extensions.takeIfContent
import app.dapk.st.design.components.*
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomEvent.Message
import app.dapk.st.matrix.sync.RoomState
import app.dapk.st.messenger.gallery.ImageGalleryActivityPayload
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.navigator.Navigator
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun MessengerScreen(
    roomId: RoomId,
    attachments: List<MessageAttachment>?,
    viewModel: MessengerViewModel,
    navigator: Navigator,
    galleryLauncher: ActivityResultLauncher<ImageGalleryActivityPayload>
) {
    val state = viewModel.state

    viewModel.ObserveEvents(galleryLauncher)
    LifecycleEffect(
        onStart = { viewModel.post(MessengerAction.OnMessengerVisible(roomId, attachments)) },
        onStop = { viewModel.post(MessengerAction.OnMessengerGone) }
    )

    val roomTitle = when (val roomState = state.roomState) {
        is Lce.Content -> roomState.value.roomState.roomOverview.roomName
        else -> null
    }

    val replyActions = ReplyActions(
        onReply = { viewModel.post(MessengerAction.ComposerEnterReplyMode(it)) },
        onDismiss = { viewModel.post(MessengerAction.ComposerExitReplyMode) }
    )

    Column {
        Toolbar(onNavigate = { navigator.navigate.upToHome() }, roomTitle, actions = {
//            OverflowMenu {
//                DropdownMenuItem(text = { Text("Settings", color = MaterialTheme.colorScheme.onSecondaryContainer) }, onClick = {})
//            }
        })
        when (state.composerState) {
            is ComposerState.Text -> {
                Room(state.roomState, replyActions, onRetry = { viewModel.post(MessengerAction.OnMessengerVisible(roomId, attachments)) })
                TextComposer(
                    state.composerState,
                    onTextChange = { viewModel.post(MessengerAction.ComposerTextUpdate(it)) },
                    onSend = { viewModel.post(MessengerAction.ComposerSendText) },
                    onAttach = { viewModel.startAttachment() },
                    replyActions = replyActions,
                )
            }

            is ComposerState.Attachments -> {
                AttachmentComposer(
                    state.composerState,
                    onSend = { viewModel.post(MessengerAction.ComposerSendText) },
                    onCancel = { viewModel.post(MessengerAction.ComposerClear) }
                )
            }
        }
    }
}

@Composable
private fun MessengerViewModel.ObserveEvents(galleryLauncher: ActivityResultLauncher<ImageGalleryActivityPayload>) {
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                MessengerEvent.SelectImageAttachment -> {
                    state.roomState.takeIfContent()?.let {
                        galleryLauncher.launch(ImageGalleryActivityPayload(it.roomState.roomOverview.roomName ?: ""))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Room(roomStateLce: Lce<MessengerState>, replyActions: ReplyActions, onRetry: () -> Unit) {
    when (val state = roomStateLce) {
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            RoomContent(state.value.self, state.value.roomState, replyActions)
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        is Lce.Error -> GenericError(cause = state.cause, action = onRetry)
    }
}

@Composable
private fun ColumnScope.RoomContent(self: UserId, state: RoomState, replyActions: ReplyActions) {
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
            AlignedBubble(item, self, wasPreviousMessageSameSender, replyActions) {
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
    replyActions: ReplyActions,
    content: @Composable (BubbleContent<T>) -> Unit
) {
    when (message.author.id == self) {
        true -> {
            Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillParentMaxWidth(0.85f), contentAlignment = Alignment.TopEnd) {
                    Bubble(
                        message = message,
                        isNotSelf = false,
                        wasPreviousMessageSameSender = wasPreviousMessageSameSender,
                        replyActions = replyActions,
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
                    wasPreviousMessageSameSender = wasPreviousMessageSameSender,
                    replyActions = replyActions,
                ) {
                    content(BubbleContent(othersBackgroundShape, SmallTalkTheme.extendedColors.othersBubble, true, message))
                }
            }
        }
    }
}

@Composable
private fun MessageImage(content: BubbleContent<RoomEvent.Image>) {
    val context = LocalContext.current

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
                        color = content.textColor()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Image(
                    modifier = Modifier.size(content.message.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .fetcherFactory(LocalDecyptingFetcherFactory.current)
                            .memoryCacheKey(content.message.imageMeta.url)
                            .data(content.message)
                            .build()
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
                        color = content.textColor(),
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
    replyActions: ReplyActions,
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
                            replyActions.onReply(message)
                        }
                    }

                    coroutineScope.launch {
                        offsetX.animateTo(targetValue = 0f)
                    }
                }
            )
    ) {
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
private fun BubbleContent<*>.textColor(): Color {
    return if (this.isNotSelf) SmallTalkTheme.extendedColors.onOthersBubble else SmallTalkTheme.extendedColors.onSelfBubble
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
                        color = content.textColor()
                    )
                }
                Text(
                    text = content.message.content,
                    color = content.textColor(),
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
                        color = content.textColor(),
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
                val context = LocalContext.current
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (content.isNotSelf) SmallTalkTheme.extendedColors.onOthersBubble.copy(alpha = 0.1f) else SmallTalkTheme.extendedColors.onSelfBubble.copy(
                                alpha = 0.2f
                            ), RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    val replyName = if (!content.isNotSelf && content.message.replyingToSelf) "You" else content.message.replyingTo.author.displayName
                        ?: content.message.replyingTo.author.id.value
                    Text(
                        fontSize = 11.sp,
                        text = replyName,
                        maxLines = 1,
                        color = content.textColor()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    when (val replyingTo = content.message.replyingTo) {
                        is Message -> {
                            Text(
                                text = replyingTo.content,
                                color = content.textColor().copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.wrapContentSize(),
                                textAlign = TextAlign.Start,
                            )
                        }

                        is RoomEvent.Image -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Image(
                                modifier = Modifier.size(replyingTo.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .fetcherFactory(LocalDecyptingFetcherFactory.current)
                                        .memoryCacheKey(replyingTo.imageMeta.url)
                                        .data(replyingTo)
                                        .build()
                                ),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        is RoomEvent.Reply -> {
                            // TODO - a reply to a reply
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (content.isNotSelf) {
                    Text(
                        fontSize = 11.sp,
                        text = content.message.message.author.displayName ?: content.message.message.author.id.value,
                        maxLines = 1,
                        color = content.textColor()
                    )
                }
                when (val message = content.message.message) {
                    is Message -> {
                        Text(
                            text = message.content,
                            color = content.textColor(),
                            fontSize = 15.sp,
                            modifier = Modifier.wrapContentSize(),
                            textAlign = TextAlign.Start,
                        )
                    }

                    is RoomEvent.Image -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            modifier = Modifier.size(message.imageMeta.scale(LocalDensity.current, LocalConfiguration.current)),
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(message)
                                    .memoryCacheKey(message.imageMeta.url)
                                    .fetcherFactory(LocalDecyptingFetcherFactory.current)
                                    .build()
                            ),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    is RoomEvent.Reply -> {
                        // TODO - a reply to a reply
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        fontSize = 9.sp,
                        text = content.message.time,
                        textAlign = TextAlign.End,
                        color = content.textColor(),
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
                            .border(0.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                            .size(10.dp)
                            .padding(2.dp)
                    ) {
                        if (isSent) {
                            Icon(imageVector = Icons.Filled.Check, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                is MessageMeta.LocalEcho.State.Error -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, MaterialTheme.colorScheme.error, CircleShape)
                            .size(10.dp)
                            .padding(2.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, "", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TextComposer(state: ComposerState.Text, onTextChange: (String) -> Unit, onSend: () -> Unit, onAttach: () -> Unit, replyActions: ReplyActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min), verticalAlignment = Alignment.Bottom
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .background(SmallTalkTheme.extendedColors.othersBubble, RoundedCornerShape(24.dp)),
        ) {
            AnimatedContent(
                targetState = state.reply,
                transitionSpec = {
                    val durationMillis = 300
                    slideIntoContainer(towards = AnimatedContentScope.SlideDirection.Up, tween(durationMillis)) { it / 2 }
                        .with(slideOutVertically(tween(durationMillis)) { it / 2 })
                        .using(
                            SizeTransform(
                                clip = true,
                                sizeAnimationSpec = { _, _ -> tween(durationMillis) })
                        )
                }
            ) {
                if (it is Message) {
                    Box(Modifier.padding(12.dp)) {
                        Box(Modifier.padding(8.dp).clickable { replyActions.onDismiss() }.wrapContentWidth().align(Alignment.TopEnd)) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Filled.Close,
                                contentDescription = "",
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(SmallTalkTheme.extendedColors.onOthersBubble.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            val replyName = it.author.displayName ?: it.author.id.value
                            Text(
                                fontSize = 11.sp,
                                text = replyName,
                                maxLines = 1,
                                color = SmallTalkTheme.extendedColors.onOthersBubble
                            )

                            Text(
                                text = it.content,
                                color = SmallTalkTheme.extendedColors.onOthersBubble,
                                fontSize = 14.sp,
                                maxLines = 2,
                                modifier = Modifier.wrapContentSize(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.background(SmallTalkTheme.extendedColors.othersBubble, RoundedCornerShape(24.dp))) {
                BasicTextField(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    value = state.value,
                    onValueChange = { onTextChange(it) },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = LocalTextStyle.current.copy(color = SmallTalkTheme.extendedColors.onOthersBubble),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrect = true),
                    decorationBox = { innerField ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                if (state.value.isEmpty()) {
                                    Text("Message", color = SmallTalkTheme.extendedColors.onOthersBubble.copy(alpha = 0.5f))
                                }
                                innerField()
                            }
                            Icon(
                                modifier = Modifier.clickable { onAttach() }.wrapContentWidth().align(Alignment.Bottom),
                                imageVector = Icons.Filled.Image,
                                contentDescription = "",
                            )
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        var size by remember { mutableStateOf(IntSize(0, 0)) }
        val enabled = state.value.isNotEmpty()
        IconButton(
            enabled = enabled,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
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
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun AttachmentComposer(state: ComposerState.Attachments, onSend: () -> Unit, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        Image(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .align(Alignment.Center),
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(state.values.first().uri.value.toUri())
                    .build()
            ),
            contentDescription = null,
        )

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            IconButton(
                enabled = true,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                onClick = onSend,
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

class ReplyActions(
    val onReply: (RoomEvent) -> Unit,
    val onDismiss: () -> Unit,
)
