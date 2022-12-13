package app.dapk.st.login

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import app.dapk.st.login.state.LoginState
import app.dapk.st.login.state.loginReducer
import app.dapk.st.push.PushModule
import app.dapk.st.state.createStateViewModel

class LoginModule(
    private val chatEngine: ChatEngine,
    private val pushModule: PushModule,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun loginState(): LoginState {
        return createStateViewModel {
            loginReducer(chatEngine, pushModule.pushTokenRegistrar(), errorTracker, it)
        }
    }
}