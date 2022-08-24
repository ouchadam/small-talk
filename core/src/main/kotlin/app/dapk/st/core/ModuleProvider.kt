package app.dapk.st.core

import kotlin.reflect.KClass

interface ModuleProvider {

    fun <T : ProvidableModule> provide(klass: KClass<T>): T
    fun reset()
}

interface ProvidableModule