package app.dapk.st.core.page

import app.dapk.st.design.components.SpiderPage
import app.dapk.state.*
import kotlin.reflect.KClass

fun <P : Any> createPageReducer(
    initialPage: SpiderPage<out P>
): ReducerFactory<PageContainer<P>> {
    return createReducer(
        initialState = PageContainer(
            page = initialPage
        ),

        change(PageAction.GoTo::class) { action, state ->
            state.copy(page = action.page as SpiderPage<P>)
        },

        change(PageAction.UpdatePage::class) { action, state ->
            val isSamePage = state.page.state::class == action.pageContent::class
            if (isSamePage) {
                val updatedPageContent = (state.page as SpiderPage<Any>).copy(state = action.pageContent)
                state.copy(page = updatedPageContent as SpiderPage<out P>)
            } else {
                state
            }
        },
    )
}

sealed interface PageAction<P> : Action {
    data class GoTo<P : Any>(val page: SpiderPage<P>) : PageAction<P>
    data class UpdatePage<P : Any>(val pageContent: P) : PageAction<P>
}

data class PageContainer<P>(
    val page: SpiderPage<out P>
)

fun PageContainer<*>.isDifferentPage(page: SpiderPage<*>): Boolean {
    return page::class != this.page::class
}

interface PageReducerScope {
    fun <PC : Any> withPageContent(page: KClass<PC>, block: PageDispatchScope<PC>.() -> Unit)
}

interface PageDispatchScope<P> {
    fun ReducerScope<*>.pageDispatch(action: PageAction<P>)
    fun getPageState(): P?
}

fun <P : Any, S : Any> createPageReducer(
    initialPage: SpiderPage<out P>,
    factory: PageReducerScope.() -> ReducerFactory<S>,
): ReducerFactory<Combined2<PageContainer<P>, S>> = shareState {
    combineReducers(
        createPageReducer(initialPage),
        factory(object : PageReducerScope {
            override fun <PC : Any> withPageContent(page: KClass<PC>, block: PageDispatchScope<PC>.() -> Unit) {
                val currentPage = getSharedState().state1.page.state
                if (currentPage::class == page) {
                    val pageDispatchScope = object : PageDispatchScope<PC> {
                        override fun ReducerScope<*>.pageDispatch(action: PageAction<PC>) {
                            val currentPageGuard = getSharedState().state1.page.state
                            if (currentPageGuard::class == page) {
                                dispatch(action)
                            }
                        }

                        override fun getPageState() = getSharedState().state1.page.state as? PC
                    }
                    block(pageDispatchScope)
                }
            }
        })
    )
}

inline fun <reified PC : Any> PageReducerScope.withPageContext(crossinline block: PageDispatchScope<PC>.(PC) -> Unit) {
    withPageContent(PC::class) { getPageState()?.let { block(it) } }
}

