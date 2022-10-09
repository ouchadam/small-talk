package app.dapk.st.login

import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import app.dapk.st.push.PushModule

class LoginModule(
    private val chatEngine: ChatEngine,
    private val pushModule: PushModule,
    private val errorTracker: ErrorTracker,
) : ProvidableModule {

    fun loginViewModel(): LoginViewModel {
        return LoginViewModel(chatEngine, pushModule.pushTokenRegistrar(), errorTracker)
    }
}