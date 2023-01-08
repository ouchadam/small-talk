package app.dapk.st.login

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import app.dapk.st.login.state.*
import app.dapk.st.push.PushModule
import app.dapk.st.state.createStateViewModel
import app.dapk.state.ReducerFactory

class LoginModule(
    private val chatEngine: ChatEngine,
    private val pushModule: PushModule,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun loginState(): LoginState {
        return createStateViewModel {
            loginReducer(it)
        }
    }

    fun loginReducer(eventEmitter: suspend (LoginEvent) -> Unit): ReducerFactory<LoginScreenState> {
        val loginUseCase = LoginUseCase(chatEngine, pushModule.pushTokenRegistrars(), errorTracker)
        return loginReducer(loginUseCase, eventEmitter)
    }
}