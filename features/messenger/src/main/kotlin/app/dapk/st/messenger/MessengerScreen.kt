package app.dapk.st.messenger

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
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
import app.dapk.st.engine.MessageMeta
import app.dapk.st.engine.MessengerState
import app.dapk.st.engine.RoomEvent
import app.dapk.st.engine.RoomState
import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.messenger.gallery.ImageGalleryActivityPayload
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.navigator.Navigator
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch

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

    val messageActions = MessageActions(
        onReply = { viewModel.post(MessengerAction.ComposerEnterReplyMode(it)) },
        onDismiss = { viewModel.post(MessengerAction.ComposerExitReplyMode) },
        onLongClick = { viewModel.post(MessengerAction.CopyToClipboard(it)) }
    )

    Column {
        Toolbar(onNavigate = { navigator.navigate.upToHome() }, roomTitle, actions = {
//            OverflowMenu {
//                DropdownMenuItem(text = { Text("Settings", color = MaterialTheme.colorScheme.onSecondaryContainer) }, onClick = {})
//            }
        })
        when (state.composerState) {
            is ComposerState.Text -> {
                Room(state.roomState, messageActions, onRetry = { viewModel.post(MessengerAction.OnMessengerVisible(roomId, attachments)) })
                TextComposer(
                    state.composerState,
                    onTextChange = { viewModel.post(MessengerAction.ComposerTextUpdate(it)) },
                    onSend = { viewModel.post(MessengerAction.ComposerSendText) },
                    onAttach = { viewModel.startAttachment() },
                    messageActions = messageActions,
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
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                MessengerEvent.SelectImageAttachment -> {
                    state.roomState.takeIfContent()?.let {
                        galleryLauncher.launch(ImageGalleryActivityPayload(it.roomState.roomOverview.roomName ?: ""))
                    }
                }

                is MessengerEvent.Toast -> {
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Room(roomStateLce: Lce<MessengerState>, messageActions: MessageActions, onRetry: () -> Unit) {
    when (val state = roomStateLce) {
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            RoomContent(state.value.self, state.value.roomState, messageActions)
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
private fun ColumnScope.RoomContent(self: UserId, state: RoomState, messageActions: MessageActions) {
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

            AlignedDraggableContainer(
                avatar = Avatar(item.author.avatarUrl?.value, item.author.displayName ?: item.author.id.value),
                isSelf = self == item.author.id,
                wasPreviousMessageSameSender = wasPreviousMessageSameSender,
                onReply = { messageActions.onReply(item) },
            ) {
                val status = @Composable { SendStatus(item) }
                MessageBubble(this, item.toModel(), status, onLongClick = messageActions.onLongClick)
            }
        }
    }
}

@Composable
private fun RoomEvent.toModel(): BubbleModel {
    val event = BubbleModel.Event(this.author.id.value, this.author.displayName ?: this.author.id.value, this.edited, this.time)
    return when (this) {
        is RoomEvent.Message -> BubbleModel.Text(this.content.toApp(), event)
        is RoomEvent.Encrypted -> BubbleModel.Encrypted(event)
        is RoomEvent.Image -> {
            val imageRequest = LocalImageRequestFactory.current
                .memoryCacheKey(this.imageMeta.url)
                .data(this)
                .build()
            val imageContent = BubbleModel.Image.ImageContent(this.imageMeta.width, this.imageMeta.height, this.imageMeta.url)
            BubbleModel.Image(imageContent, imageRequest, event)
        }

        is RoomEvent.Reply -> {
            BubbleModel.Reply(this.replyingTo.toModel(), this.message.toModel())
        }
    }
}

private fun RichText.toApp(): app.dapk.st.core.RichText {
    return app.dapk.st.core.RichText(this.parts.map {
        when (it) {
            is RichText.Part.Bold -> app.dapk.st.core.RichText.Part.Bold(it.content)
            is RichText.Part.BoldItalic -> app.dapk.st.core.RichText.Part.BoldItalic(it.content)
            is RichText.Part.Italic -> app.dapk.st.core.RichText.Part.Italic(it.content)
            is RichText.Part.Link -> app.dapk.st.core.RichText.Part.Link(it.url, it.label)
            is RichText.Part.Normal -> app.dapk.st.core.RichText.Part.Normal(it.content)
            is RichText.Part.Person -> app.dapk.st.core.RichText.Part.Person(it.userId.value)
        }
    }.toSet())
}

@Composable
private fun SendStatus(message: RoomEvent) {
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
private fun TextComposer(state: ComposerState.Text, onTextChange: (String) -> Unit, onSend: () -> Unit, onAttach: () -> Unit, messageActions: MessageActions) {
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
                if (it is RoomEvent.Message) {
                    Box(Modifier.padding(12.dp)) {
                        Box(Modifier.padding(8.dp).clickable { messageActions.onDismiss() }.wrapContentWidth().align(Alignment.TopEnd)) {
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
                                text = it.content.toApp().toAnnotatedText(),
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

class MessageActions(
    val onReply: (RoomEvent) -> Unit,
    val onDismiss: () -> Unit,
    val onLongClick: (BubbleModel) -> Unit,
)
