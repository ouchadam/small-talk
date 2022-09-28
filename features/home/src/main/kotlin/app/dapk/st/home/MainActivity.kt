package app.dapk.st.home

import android.Manifest
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.PermissionResult
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.design.components.Toolbar
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.home.gallery.FetchMediaFoldersUseCase
import app.dapk.st.home.gallery.Folder
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch

class MainActivity : DapkActivity() {

    private val directoryViewModel by viewModel { module<DirectoryModule>().directoryViewModel() }
    private val loginViewModel by viewModel { module<LoginModule>().loginViewModel() }
    private val profileViewModel by viewModel { module<ProfileModule>().profileViewModel() }
    private val homeViewModel by viewModel { module<HomeModule>().homeViewModel(directoryViewModel, loginViewModel, profileViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val state = mutableStateOf(emptyList<Folder>())

        lifecycleScope.launch {
            when (ensurePermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionResult.Denied -> {
                }

                PermissionResult.Granted -> {
                    state.value = FetchMediaFoldersUseCase(contentResolver).fetchFolders()
                }

                PermissionResult.ShowRational -> {

                }
            }

        }

        setContent {
            Surface {
                ImageGallery(state)
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

@Composable
fun ImageGallery(state: State<List<Folder>>) {
    var boxWidth by remember { mutableStateOf(IntSize.Zero) }
    val localDensity = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp

    Column {
        Toolbar(title = "Send to Awesome Room", onNavigate = {})
        val columns = when {
            screenWidth > 600 -> 4
            else -> 2
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.value, key = { it.bucketId }) {
                Box(modifier = Modifier.fillMaxWidth().padding(2.dp).onGloballyPositioned {
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
