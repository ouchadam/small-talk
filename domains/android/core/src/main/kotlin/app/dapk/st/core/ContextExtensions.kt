package app.dapk.st.core

import android.content.Context

inline fun <reified T : ProvidableModule> Context.module() =
    (this.applicationContext as ModuleProvider).provide(T::class)