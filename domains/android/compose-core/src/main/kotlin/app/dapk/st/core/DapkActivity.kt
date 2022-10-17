package app.dapk.st.core

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.lifecycleScope
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.design.components.ThemeConfig
import app.dapk.st.navigator.navigator
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.activity.compose.setContent as _setContent

abstract class DapkActivity : ComponentActivity(), EffectScope {

    private val coreAndroidModule by unsafeLazy { module<CoreAndroidModule>() }
    private val themeStore by unsafeLazy { coreAndroidModule.themeStore() }
    private val remembers = mutableMapOf<Any, Any>()
    protected val navigator by navigator { coreAndroidModule.intentFactory() }

    private lateinit var themeConfig: ThemeConfig

    private val needsBackLeakWorkaround = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.themeConfig = runBlocking { ThemeConfig(themeStore.isMaterialYouEnabled()) }

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    protected fun setContent(content: @Composable () -> Unit) {
        _setContent {
            SmallTalkTheme(themeConfig) {
                content()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (themeConfig.useDynamicTheme != themeStore.isMaterialYouEnabled()) {
                recreate()
            }
        }
    }

    @Composable
    override fun OnceEffect(key: Any, sideEffect: () -> Unit) {
        val triggerSideEffect = remembers.containsKey(key).not()
        if (triggerSideEffect) {
            remembers[key] = Unit
            SideEffect {
                sideEffect()
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (needsBackLeakWorkaround && !onBackPressedDispatcher.hasEnabledCallbacks()) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    protected fun registerForPermission(permission: String, callback: () -> Unit = {}): () -> Unit {
        val resultCallback: (result: Boolean) -> Unit = { result ->
            if (result) {
                callback()
            }
        }
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission(), resultCallback)
        return { launcher.launch(permission) }
    }

    protected suspend fun ensurePermission(permission: String): PermissionResult {
        return when {
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> PermissionResult.Granted

            shouldShowRequestPermissionRationale(permission) -> PermissionResult.ShowRational

            else -> {
                val isGranted = suspendCancellableCoroutine { continuation ->
                    val callback: (result: Boolean) -> Unit = { result -> continuation.resume(result) }
                    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission(), callback)
                    launcher.launch(permission)
                    continuation.invokeOnCancellation { launcher.unregister() }
                }

                when (isGranted) {
                    true -> PermissionResult.Granted
                    false -> PermissionResult.Denied
                }
            }
        }
    }
}

sealed interface PermissionResult {
    object Granted : PermissionResult
    object ShowRational : PermissionResult
    object Denied : PermissionResult
}
