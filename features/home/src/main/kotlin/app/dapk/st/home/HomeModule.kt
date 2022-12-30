package app.dapk.st.home

import app.dapk.st.core.JobBag
import app.dapk.st.core.ProvidableModule
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.domain.StoreModule
import app.dapk.st.engine.ChatEngine
import app.dapk.st.home.state.homeReducer
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule
import app.dapk.st.state.State
import app.dapk.st.state.createStateViewModel
import app.dapk.state.Action
import app.dapk.state.DynamicReducers
import app.dapk.state.combineReducers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

class HomeModule(
    private val chatEngine: ChatEngine,
    private val storeModule: StoreModule,
    val betaVersionUpgradeUseCase: BetaVersionUpgradeUseCase,
    private val profileModule: ProfileModule,
    private val loginModule: LoginModule,
    private val directoryModule: DirectoryModule,
) : ProvidableModule {

    internal fun compositeHomeState(): DynamicState {
        return createStateViewModel {
            combineReducers(
                listOf(
                    homeReducerFactory(it),
                    loginModule.loginReducer(it),
                    profileModule.profileReducer(),
                    directoryModule.directoryReducer(it)
                )
            )
        }
    }

    private fun homeReducerFactory(eventEmitter: suspend (Any) -> Unit) =
        homeReducer(chatEngine, storeModule.cacheCleaner(), betaVersionUpgradeUseCase, JobBag(), eventEmitter)
}

typealias DynamicState = State<DynamicReducers, Any>

inline fun <reified S, reified E> DynamicState.childState() = object : State<S, E> {
    override fun dispatch(action: Action) = this@childState.dispatch(action)
    override val events: Flow<E> = this@childState.events.filterIsInstance()
    override val current: S = this@childState.current.getState()
}
