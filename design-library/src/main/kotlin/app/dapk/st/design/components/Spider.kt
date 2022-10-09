package app.dapk.st.design.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <T : Any> Spider(currentPage: SpiderPage<T>, onNavigate: (SpiderPage<out T>?) -> Unit, graph: SpiderScope.() -> Unit) {
    val pageCache = remember { mutableMapOf<Route<*>, SpiderPage<out T>>() }
    pageCache[currentPage.route] = currentPage

    val navigateAndPopStack = {
        pageCache.remove(currentPage.route)
        onNavigate(pageCache[currentPage.parent])
    }
    val itemScope = object : SpiderItemScope {
        override fun goBack() {
            navigateAndPopStack()
        }
    }

    val computedWeb = remember(true) {
        mutableMapOf<Route<*>, @Composable (T) -> Unit>().also { computedWeb ->
            val scope = object : SpiderScope {
                override fun <T> item(route: Route<T>, content: @Composable SpiderItemScope.(T) -> Unit) {
                    computedWeb[route] = { content(itemScope, it as T) }
                }
            }
            graph.invoke(scope)
        }
    }

    Column {
        if (currentPage.hasToolbar) {
            Toolbar(
                onNavigate = navigateAndPopStack,
                title = currentPage.label
            )
        }
        BackHandler(onBack = navigateAndPopStack)
        computedWeb[currentPage.route]!!.invoke(currentPage.state)
    }
}


interface SpiderScope {
    fun <T> item(route: Route<T>, content: @Composable SpiderItemScope.(T) -> Unit)
}

interface SpiderItemScope {
    fun goBack()
}

data class SpiderPage<T>(
    val route: Route<T>,
    val label: String,
    val parent: Route<*>?,
    val state: T,
    val hasToolbar: Boolean = true,
)

@JvmInline
value class Route<S>(val value: String)