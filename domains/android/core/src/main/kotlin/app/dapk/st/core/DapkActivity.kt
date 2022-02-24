package app.dapk.st.core

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.navigator.navigator

abstract class DapkActivity : ComponentActivity(), EffectScope {

    private val coreAndroidModule by unsafeLazy { module<CoreAndroidModule>() }
    private val remembers = mutableMapOf<Any, Any>()
    protected val navigator by navigator { coreAndroidModule.intentFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
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
}
