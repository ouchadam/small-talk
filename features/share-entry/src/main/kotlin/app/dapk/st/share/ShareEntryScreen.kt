package app.dapk.st.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.MimeType
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.CircleishAvatar
import app.dapk.st.design.components.GenericEmpty
import app.dapk.st.design.components.GenericError
import app.dapk.st.design.components.Toolbar
import app.dapk.st.navigator.MessageAttachment
import app.dapk.st.navigator.Navigator
import app.dapk.st.share.DirectoryScreenState.*

@Composable
fun ShareEntryScreen(navigator: Navigator, viewModel: ShareEntryViewModel) {
    val state = viewModel.state
    viewModel.ObserveEvents(navigator)

    LifecycleEffect(
        onStart = { viewModel.start() },
        onStop = { viewModel.stop() }
    )

    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Toolbar(title = "Send to...")
        when (state) {
            EmptyLoading -> CenteredLoading()
            Empty -> GenericEmpty()
            is Error -> GenericError {
                // TODO
            }
            is Content -> Content(listState, state) {
                viewModel.onRoomSelected(it)
            }
        }
    }
}

@Composable
private fun ShareEntryViewModel.ObserveEvents(navigator: Navigator) {
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                is DirectoryEvent.SelectRoom -> {
                    navigator.navigate.toMessenger(it.item.id, it.uris.map { MessageAttachment(it, MimeType.Image) })
                }
            }
        }
    }
}


@Composable
private fun Content(listState: LazyListState, state: Content, onClick: (Item) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = 72.dp)) {
        items(
            items = state.items,
            key = { it.id.value },
        ) {
            DirectoryItem(it, onClick = onClick)
        }
    }
}

@Composable
private fun DirectoryItem(item: Item, onClick: (Item) -> Unit) {
    val roomName = item.roomName

    Box(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .clickable { onClick(item) }
    ) {
        Row(Modifier.padding(20.dp)) {
            val secondaryText = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)

            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                CircleishAvatar(item.roomAvatarUrl?.value, roomName, size = 50.dp)
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
                }
                Text(text = item.members.joinToString(), color = secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

