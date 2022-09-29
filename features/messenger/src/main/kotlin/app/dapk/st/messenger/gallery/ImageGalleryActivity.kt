package app.dapk.st.messenger.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.Lce
import app.dapk.st.core.PermissionResult
import kotlinx.coroutines.launch

class ImageGalleryActivity : DapkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ImageGalleryViewModel(
            FetchMediaFoldersUseCase(contentResolver),
            FetchMediaUseCase(contentResolver),
        )

        val permissionState = mutableStateOf<Lce<PermissionResult>>(Lce.Loading())

        lifecycleScope.launch {
            permissionState.value = runCatching { ensurePermission(Manifest.permission.READ_EXTERNAL_STORAGE) }.fold(
                onSuccess = { Lce.Content(it) },
                onFailure = { Lce.Error(it) }
            )
        }

        setContent {
            Surface {
                PermissionGuard(permissionState) {
                    ImageGalleryScreen(viewModel, onTopLevelBack = { finish() }) { media ->
                        setResult(RESULT_OK, Intent().setData(media.uri))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun Activity.PermissionGuard(state: State<Lce<PermissionResult>>, onGranted: @Composable () -> Unit) {
    when (val content = state.value) {
        is Lce.Content -> when (content.value) {
            PermissionResult.Granted -> onGranted()
            PermissionResult.Denied -> finish()
            PermissionResult.ShowRational -> finish()
        }

        is Lce.Error -> finish()
        is Lce.Loading -> {
            // loading should be quick, let's avoid displaying anything
        }
    }

}

class GetImageFromGallery : ActivityResultContract<Void?, Uri?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(context, ImageGalleryActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}