package app.dapk.st.login.state

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.logP
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.LoginRequest
import app.dapk.st.engine.LoginResult
import app.dapk.st.push.PushTokenRegistrar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class LoginUseCase(
    private val chatEngine: ChatEngine,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val errorTracker: ErrorTracker,
) {
    suspend fun login(request: LoginRequest): LoginResult {
        return logP("login") {
            when (val result = chatEngine.login(request)) {
                is LoginResult.Success -> {
                    coroutineScope {
                        runCatching {
                            listOf(
                                async { pushTokenRegistrar.registerCurrentToken() },
                                async { chatEngine.preloadMe() },
                            ).awaitAll()
                        }
                        result
                    }
                }

                is LoginResult.Error -> {
                    errorTracker.track(result.cause)
                    result
                }

                LoginResult.MissingWellKnown -> result
            }
        }
    }

    private suspend fun ChatEngine.preloadMe() = this.me(forceRefresh = false)
}