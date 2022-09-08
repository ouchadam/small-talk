package app.dapk.st.directory

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
import app.dapk.st.design.components.GenericEmpty
import app.dapk.st.design.components.GenericError
import app.dapk.st.design.components.Toolbar
import app.dapk.st.directory.DirectoryEvent.OpenDownloadUrl
import app.dapk.st.directory.DirectoryScreenState.Content
import app.dapk.st.directory.DirectoryScreenState.EmptyLoading
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.messenger.MessengerActivity
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun DirectoryScreen(directoryViewModel: DirectoryViewModel) {
    val state = directoryViewModel.state

    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
    )

    val toolbarHeight = 72.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }

    directoryViewModel.ObserveEvents(listState, toolbarOffsetHeightPx)

    LifecycleEffect(
        onStart = { directoryViewModel.start() },
        onStop = { directoryViewModel.stop() }
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        when (state) {
            EmptyLoading -> CenteredLoading()
            DirectoryScreenState.Empty -> GenericEmpty()
            is Error -> GenericError {
                // TODO
            }
            is Content -> Content(listState, state)
        }
        Toolbar(title = "Messages", offset = { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) })
    }
}

@Composable
private fun DirectoryViewModel.ObserveEvents(listState: LazyListState, toolbarPosition: MutableState<Float>) {
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                is OpenDownloadUrl -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
                }
                DirectoryEvent.ScrollToTop -> {
                    toolbarPosition.value = 0f
                    listState.scrollToItem(0)
                }
            }
        }
    }
}


val clock = Clock.systemUTC()

@Composable
private fun Content(listState: LazyListState, state: Content) {
    val context = LocalContext.current
    val navigateToRoom = { roomId: RoomId ->
        context.startActivity(MessengerActivity.newInstance(context, roomId))
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = state.overviewState) {
        if (listState.firstVisibleItemScrollOffset <= 1) {
            scope.launch { listState.scrollToItem(0) }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = 72.dp)) {
        items(
            items = state.overviewState,
            key = { it.overview.roomId.value },
        ) {
            DirectoryItem(it, onClick = navigateToRoom, clock)
        }
    }
}

@Composable
private fun DirectoryItem(room: RoomFoo, onClick: (RoomId) -> Unit, clock: Clock) {
    val overview = room.overview
    val roomName = overview.roomName ?: "Empty room"
    val hasUnread = room.unreadCount.value > 0

    Box(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .clickable {
                onClick(overview.roomId)
            }) {
        Row(Modifier.padding(20.dp)) {
            val secondaryText = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                CircleishAvatar(overview.roomAvatarUrl?.value, roomName, size = 50.dp)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        fontSize = 18.sp,
                        text = roomName,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val formattedTimestamp = remember(overview.lastMessage) {
                        overview.lastMessage?.utcTimestamp?.timeOrDate(clock.instant()) ?: overview.roomCreationUtc.timeOrDate(clock.instant())
                    }

                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        fontSize = 12.sp,
                        maxLines = 1,
                        text = formattedTimestamp,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else secondaryText
                    )
                }

                if (hasUnread) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            body(overview, secondaryText, room.typing)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(Modifier.align(Alignment.CenterVertically)) {
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val unreadTextSize = when (room.unreadCount.value > 99) {
                                    true -> 9.sp
                                    false -> 10.sp
                                }
                                val unreadLabelContent = when {
                                    room.unreadCount.value > 99 -> "99+"
                                    else -> room.unreadCount.value.toString()
                                }
                                Text(
                                    fontSize = unreadTextSize,
                                    fontWeight = FontWeight.Medium,
                                    text = unreadLabelContent,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    body(overview, secondaryText, room.typing)
                }
            }
        }
    }
}

@Composable
private fun body(overview: RoomOverview, secondaryText: Color, typing: SyncService.SyncEvent.Typing?) {
    val bodySize = 14.sp

    when {
        typing != null && typing.members.isNotEmpty() -> {
            Text(
                overflow = TextOverflow.Ellipsis,
                fontSize = bodySize,
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
        else -> when (val lastMessage = overview.lastMessage) {
            null -> {
                Text(
                    fontSize = bodySize,
                    text = "",
                    maxLines = 1,
                    color = secondaryText
                )
            }
            else -> {
                when (overview.isGroup) {
                    true -> {
                        val displayName = lastMessage.author.displayName ?: lastMessage.author.id.value
                        Text(
                            overflow = TextOverflow.Ellipsis,
                            fontSize = bodySize,
                            text = "$displayName: ${lastMessage.content}",
                            maxLines = 1,
                            color = secondaryText
                        )
                    }
                    false -> {
                        Text(
                            overflow = TextOverflow.Ellipsis,
                            fontSize = bodySize,
                            text = lastMessage.content,
                            maxLines = 1,
                            color = secondaryText
                        )
                    }
                }
            }
        }
    }
}

internal val DEFAULT_ZONE = ZoneId.systemDefault()
internal val OVERVIEW_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
internal val OVERVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Long.timeOrDate(now: Instant): String {
    return createTime(now)
}

private fun Long.createTime(now: Instant): String {
    val instant = Instant.ofEpochMilli(this)
    val format = when {
        instant.truncatedTo(ChronoUnit.DAYS) == now.truncatedTo(ChronoUnit.DAYS) -> OVERVIEW_TIME_FORMAT
        else -> OVERVIEW_DATE_FORMAT
    }
    return ZonedDateTime.ofInstant(instant, DEFAULT_ZONE).toLocalDateTime().format(format)
}