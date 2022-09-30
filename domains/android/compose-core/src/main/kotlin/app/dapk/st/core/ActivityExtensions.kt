package app.dapk.st.core

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider.*

inline fun <reified VM : ViewModel> ComponentActivity.viewModel(
    noinline factory: () -> VM
): Lazy<VM> {
    val factoryPromise = object : Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = when (modelClass) {
            VM::class.java -> factory() as T
            else -> throw Error()
        }
    }
    return ViewModelLazy(VM::class, { viewModelStore }, { factoryPromise })
}
