package app.dapk.st.core

import app.dapk.st.navigator.IntentFactory

class CoreAndroidModule(private val intentFactory: IntentFactory): ProvidableModule {

    fun intentFactory() = intentFactory

}