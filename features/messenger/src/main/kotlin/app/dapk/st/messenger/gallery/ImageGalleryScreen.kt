package app.dapk.st.messenger.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.Lce
import app.dapk.st.core.LifecycleEffect
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.GenericError
import app.dapk.st.design.components.Spider
import app.dapk.st.design.components.Toolbar
import app.dapk.st.messenger.gallery.state.ImageGalleryActions
import app.dapk.st.messenger.gallery.state.ImageGalleryPage
import app.dapk.st.messenger.gallery.state.ImageGalleryState
import app.dapk.state.SpiderPage
import app.dapk.state.page.PageAction
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun ImageGalleryScreen(state: ImageGalleryState, onTopLevelBack: () -> Unit, onImageSelected: (Media) -> Unit) {
    LifecycleEffect(onStart = {
        state.dispatch(ImageGalleryActions.Visible)
    })

    val onNavigate: (SpiderPage<out ImageGalleryPage>?) -> Unit = {
        when (it) {
            null -> onTopLevelBack()
            else -> state.dispatch(PageAction.GoTo(it))
        }
    }

    Spider(currentPage = state.current.state1.page, onNavigate = onNavigate, toolbar = { navigate, title -> Toolbar(navigate, title)  }) {
        item(ImageGalleryPage.Routes.folders) {
            ImageGalleryFolders(
                it,
                onClick = { state.dispatch(ImageGalleryActions.SelectFolder(it)) },
                onRetry = { state.dispatch(ImageGalleryActions.Visible) }
            )
        }
        item(ImageGalleryPage.Routes.files) {
            ImageGalleryMedia(it, onImageSelected, onRetry = { state.dispatch(ImageGalleryActions.SelectFolder(it.folder)) })
        }
    }
}

@Composable
private fun ImageGalleryFolders(state: ImageGalleryPage.Folders, onClick: (Folder) -> Unit, onRetry: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val gradient = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
    )

    when (val content = state.content) {
        is Lce.Loading -> {
            CenteredLoading()
        }

        is Lce.Content -> {
            Column {
                val columns = when {
                    screenWidth > 600 -> 4
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(content.value, key = { it.bucketId }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(2.dp).aspectRatio(1f)
                            .clickable { onClick(it) }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it.thumbnail.toString())
                                        .build(),
                                ),
                                contentDescription = "123",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).background(gradient).align(Alignment.BottomStart))
                            Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(it.title, fontSize = 13.sp, color = Color.White)
                                Text(it.itemCount.toString(), fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        is Lce.Error -> GenericError(cause = content.cause, action = onRetry)
    }
}

@Composable
private fun ImageGalleryMedia(state: ImageGalleryPage.Files, onFileSelected: (Media) -> Unit, onRetry: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    Column {
        val columns = when {
            screenWidth > 600 -> 4
            else -> 2
        }

        when (val content = state.content) {
            is Lce.Loading -> {
                CenteredLoading()
            }

            is Lce.Content -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val modifier = Modifier.fillMaxWidth().padding(2.dp).aspectRatio(1f)
                    items(content.value, key = { it.id }) {
                        Box(modifier = modifier.clickable { onFileSelected(it) }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it.uri.toString())
                                        .crossfade(true)
                                        .build(),
                                ),
                                contentDescription = "123",
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            is Lce.Error -> GenericError(cause = content.cause, action = onRetry)
        }

    }

}
