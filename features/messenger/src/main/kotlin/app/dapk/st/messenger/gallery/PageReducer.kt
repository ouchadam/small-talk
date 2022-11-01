package app.dapk.st.messenger.gallery

import app.dapk.st.design.components.SpiderPage
import app.dapk.state.Action
import app.dapk.state.ReducerFactory
import app.dapk.state.change
import app.dapk.state.createReducer

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

sealed interface PageAction : Action {
    data class GoTo<P : Any>(val page: SpiderPage<P>) : PageAction
    data class UpdatePage<P : Any>(val pageContent: P) : PageAction
}

data class PageContainer<P>(
    val page: SpiderPage<out P>
)
