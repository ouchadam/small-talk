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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
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

@Composable
fun DirectoryScreen(directoryViewModel: DirectoryViewModel) {
    val state = directoryViewModel.state
    directoryViewModel.ObserveEvents()
    LifecycleEffect(
        onStart = { directoryViewModel.start() },
        onStop = { directoryViewModel.stop() }
    )

    when (state) {
        is Content -> {
            Content(state)
        }
        EmptyLoading -> CenteredLoading()
        is Error -> {
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
private fun DirectoryViewModel.ObserveEvents() {
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                is OpenDownloadUrl -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
                }
            }
        }
    }
}

@Composable
private fun Content(state: Content) {
    val context = LocalContext.current
    val navigateToRoom = { roomId: RoomId ->
        context.startActivity(MessengerActivity.newInstance(context, roomId))
    }
    val clock = Clock.systemUTC()
    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = state.overviewState) {
        if (listState.firstVisibleItemScrollOffset <= 1) {
            scope.launch { listState.scrollToItem(0) }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
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
            val secondaryText = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)

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
                        color = MaterialTheme.colors.onBackground
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
                        color = if (hasUnread) MaterialTheme.colors.primary else secondaryText
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
                                    .background(color = MaterialTheme.colors.primary, shape = CircleShape)
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
                                    color = MaterialTheme.colors.onPrimary
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
                color = MaterialTheme.colors.primary
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