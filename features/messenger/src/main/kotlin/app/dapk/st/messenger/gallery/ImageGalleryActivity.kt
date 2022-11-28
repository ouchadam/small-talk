package app.dapk.st.messenger.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.*
import app.dapk.st.design.components.GenericError
import app.dapk.st.messenger.gallery.state.ImageGalleryState
import app.dapk.st.state.state
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ImageGalleryActivity : DapkActivity() {

    private val imageGalleryState: ImageGalleryState by state {
        val payload = intent.getParcelableExtra("key") as? ImageGalleryActivityPayload
        val module = module<ImageGalleryModule>()
        module.imageGalleryState(payload!!.roomName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionState = mutableStateOf<Lce<PermissionResult>>(Lce.Loading())

        lifecycleScope.launch {
            permissionState.value = runCatching { ensurePermission(mediaPermission()) }.fold(
                onSuccess = { Lce.Content(it) },
                onFailure = { Lce.Error(it) }
            )
        }

        setContent {
            Surface {
                PermissionGuard(permissionState) {
                    ImageGalleryScreen(imageGalleryState, onTopLevelBack = { finish() }) { media ->
                        setResult(RESULT_OK, Intent().setData(media.uri))
                        finish()
                    }
                }
            }
        }
    }

    private fun mediaPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
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

        is Lce.Error -> GenericError(message = "Store permission required", label = "Close") {
            finish()
        }

        is Lce.Loading -> {
            // loading should be quick, let's avoid displaying anything
        }
    }

}

class GetImageFromGallery : ActivityResultContract<ImageGalleryActivityPayload, Uri?>() {

    override fun createIntent(context: Context, input: ImageGalleryActivityPayload): Intent {
        return Intent(context, ImageGalleryActivity::class.java)
            .putExtra("key", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}


@Parcelize
data class ImageGalleryActivityPayload(
    val roomName: String,
) : Parcelable
