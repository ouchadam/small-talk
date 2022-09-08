package app.dapk.st.core

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.design.components.SmallTalkTheme
import app.dapk.st.design.components.ThemeConfig
import app.dapk.st.navigator.navigator
import androidx.activity.compose.setContent as _setContent

abstract class DapkActivity : ComponentActivity(), EffectScope {

    private val coreAndroidModule by unsafeLazy { module<CoreAndroidModule>() }
    private val themeStore by unsafeLazy { coreAndroidModule.themeStore() }
    private val remembers = mutableMapOf<Any, Any>()
    protected val navigator by navigator { coreAndroidModule.intentFactory() }

    private lateinit var themeConfig: ThemeConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.themeConfig = ThemeConfig(themeStore.isMaterialYouEnabled())

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
        if (themeConfig.useDynamicTheme != themeStore.isMaterialYouEnabled()) {
            recreate()
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
}
