package app.dapk.st.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun StartObserving(block: StartScope.() -> Unit) {
    LaunchedEffect(true) {
        block(StartScope(this))
    }
}

class StartScope(private val scope: CoroutineScope) {

    fun <T> SharedFlow<T>.launch(onEach: suspend (T) -> Unit) {
        this.onEach(onEach).launchIn(scope)
    }

    fun <T> Flow<T>.launch(onEach: suspend (T) -> Unit) {
        this.onEach(onEach).launchIn(scope)
    }
}

interface EffectScope {

    @Composable
    fun OnceEffect(key: Any, sideEffect: () -> Unit)
}


@Composable
fun LifecycleEffect(onStart: () -> Unit = {}, onStop: () -> Unit = {}) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> {
                    // ignored
                }
            }
        }

        lifecycleOwner.value.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.value.lifecycle.removeObserver(lifecycleObserver)
        }
    }
}

fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
