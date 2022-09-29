package app.dapk.st.messenger.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.*
import app.dapk.st.core.extensions.unsafeLazy
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ImageGalleryActivity : DapkActivity() {

    private val module by unsafeLazy { module<ImageGalleryModule>() }
    private val viewModel by viewModel {
        val payload = intent.getParcelableExtra("key") as? ImageGalleryActivityPayload
        module.imageGalleryViewModel(payload!!.roomName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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