package app.dapk.st.home

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.*
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.Route
import app.dapk.st.design.components.Spider
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.home.gallery.FetchMediaFoldersUseCase
import app.dapk.st.home.gallery.FetchMediaUseCase
import app.dapk.st.home.gallery.Folder
import app.dapk.st.home.gallery.Media
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.viewmodel.DapkViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : DapkActivity() {

    private val directoryViewModel by viewModel { module<DirectoryModule>().directoryViewModel() }
    private val loginViewModel by viewModel { module<LoginModule>().loginViewModel() }
    private val profileViewModel by viewModel { module<ProfileModule>().profileViewModel() }
    private val homeViewModel by viewModel { module<HomeModule>().homeViewModel(directoryViewModel, loginViewModel, profileViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ImageGalleryViewModel(
            FetchMediaFoldersUseCase(contentResolver),
            FetchMediaUseCase(contentResolver),
        )

//        lifecycleScope.launch {
//            when (ensurePermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                PermissionResult.Denied -> {
//                }
//
//                PermissionResult.Granted -> {
//                    state.value = FetchMediaFoldersUseCase(contentResolver).fetchFolders()
//                }
//
//                PermissionResult.ShowRational -> {
//
//                }
//            }
//        }

        setContent {
            Surface {
                ImageGalleryScreen(viewModel) {
                    finish()
                }
            }
        }

//        homeViewModel.events.onEach {
//            when (it) {
//                HomeEvent.Relaunch -> recreate()
//            }
//        }.launchIn(lifecycleScope)
//
//        setContent {
//            if (homeViewModel.hasVersionChanged()) {
//                BetaUpgradeDialog()
//            } else {
//                Surface(Modifier.fillMaxSize()) {
//                    HomeScreen(homeViewModel)
//                }
//            }
//        }
    }

    @Composable
    private fun BetaUpgradeDialog() {
        AlertDialog(
            title = { Text(text = "BETA") },
            text = { Text(text = "During the BETA, version upgrades require a cache clear") },
            onDismissRequest = {

            },
            confirmButton = {
                TextButton(onClick = { homeViewModel.clearCache() }) {
                    Text(text = "Clear cache".uppercase())
                }
            },
        )
    }
}


data class ImageGalleryState(
    val page: SpiderPage<out ImageGalleryPage>,
)


sealed interface ImageGalleryPage {
    data class Folders(val content: Lce<List<Folder>>) : ImageGalleryPage
    data class Files(val content: Lce<List<Media>>) : ImageGalleryPage

    object Routes {
        val folders = Route<Folders>("Folders")
        val files = Route<Files>("Files")
    }
}


sealed interface ImageGalleryEvent

class ImageGalleryViewModel(
    private val foldersUseCase: FetchMediaFoldersUseCase,
    private val fetchMediaUseCase: FetchMediaUseCase,
) : DapkViewModel<ImageGalleryState, ImageGalleryEvent>(
    initialState = ImageGalleryState(page = SpiderPage(route = ImageGalleryPage.Routes.folders, "", null, ImageGalleryPage.Folders(Lce.Loading())))
) {

    private var currentPageJob: Job? = null

    fun start() {
        currentPageJob?.cancel()
        currentPageJob = viewModelScope.launch {
            val folders = foldersUseCase.fetchFolders()
            updatePageState<ImageGalleryPage.Folders> { copy(content = Lce.Content(folders)) }
        }

    }

    fun goTo(page: SpiderPage<out ImageGalleryPage>) {
        currentPageJob?.cancel()
        updateState { copy(page = page) }
    }

    fun selectFolder(folder: Folder) {
        currentPageJob?.cancel()

        updateState {
            copy(
                page = SpiderPage(
                    route = ImageGalleryPage.Routes.files,
                    label = page.label,
                    parent = ImageGalleryPage.Routes.folders,
                    state = ImageGalleryPage.Files(Lce.Loading())
                )
            )
        }

        currentPageJob = viewModelScope.launch {
            val media = fetchMediaUseCase.getMediaInBucket(folder.bucketId)
            updatePageState<ImageGalleryPage.Files> {
                copy(content = Lce.Content(media))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : ImageGalleryPage> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }

}

@Composable
fun ImageGalleryScreen(viewModel: ImageGalleryViewModel, onTopLevelBack: () -> Unit) {
    LifecycleEffect(onStart = {
        viewModel.start()
    })

    val onNavigate: (SpiderPage<out ImageGalleryPage>?) -> Unit = {
        when (it) {
            null -> onTopLevelBack()
            else -> viewModel.goTo(it)
        }
    }

    Spider(currentPage = viewModel.state.page, onNavigate = onNavigate) {
        item(ImageGalleryPage.Routes.folders) {
            ImageGalleryFolders(it) { folder ->
                viewModel.selectFolder(folder)
            }
        }
        item(ImageGalleryPage.Routes.files) {
            ImageGalleryMedia(it)
        }
    }

}

@Composable
fun ImageGalleryFolders(state: ImageGalleryPage.Folders, onClick: (Folder) -> Unit) {
    var boxWidth by remember { mutableStateOf(IntSize.Zero) }
    val localDensity = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp

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
                        Box(modifier = Modifier.fillMaxWidth().padding(2.dp)
                            .clickable { onClick(it) }
                            .onGloballyPositioned {
                                boxWidth = it.size
                            }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it.thumbnail.toString())
                                        .build(),
                                ),
                                contentDescription = "123",
                                modifier = Modifier.fillMaxWidth().height(with(localDensity) { boxWidth.width.toDp() }),
                                contentScale = ContentScale.Crop
                            )

                            val gradient = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                startY = boxWidth.width.toFloat() * 0.5f,
                                endY = boxWidth.width.toFloat()
                            )

                            Box(modifier = Modifier.matchParentSize().background(gradient))
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

        is Lce.Error -> TODO()
    }
}

@Composable
fun ImageGalleryMedia(state: ImageGalleryPage.Files) {
    var boxWidth by remember { mutableStateOf(IntSize.Zero) }
    val localDensity = LocalDensity.current
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
                    items(content.value, key = { it.id }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(2.dp).onGloballyPositioned {
                            boxWidth = it.size
                        }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it.uri.toString())
                                        .crossfade(true)
                                        .build(),
                                ),
                                contentDescription = "123",
                                modifier = Modifier.fillMaxWidth().height(with(localDensity) { boxWidth.width.toDp() }),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            is Lce.Error -> TODO()
        }

    }

}


