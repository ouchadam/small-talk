package app.dapk.st.core

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

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


inline fun <reified S, E> ComponentActivity.state(
    noinline factory: () -> StateViewModel<S, E>
): Lazy<State<S, E>> {
    val factoryPromise = object : Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when(modelClass) {
                StateViewModel::class.java -> factory() as T
                else -> throw Error()
            }
        }
    }
    return FooViewModelLazy(
        key = S::class.java.canonicalName!!,
        StateViewModel::class,
        { viewModelStore },
        { factoryPromise }
    ) as Lazy<State<S, E>>
}

class FooViewModelLazy<VM : ViewModel> @JvmOverloads constructor(
    private val key: String,
    private val viewModelClass: KClass<VM>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory,
    private val extrasProducer: () -> CreationExtras = { CreationExtras.Empty }
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer()
                val store = storeProducer()
                ViewModelProvider(
                    store,
                    factory,
                    extrasProducer()
                ).get(key, viewModelClass.java).also {
                    cached = it
                }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}