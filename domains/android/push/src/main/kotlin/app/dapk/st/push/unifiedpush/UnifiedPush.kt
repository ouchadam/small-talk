package app.dapk.st.push.unifiedpush

import android.content.Context
import org.unifiedpush.android.connector.UnifiedPush

interface UnifiedPush {
    fun saveDistributor(distributor: String)
    fun getDistributor(): String
    fun getDistributors(): List<String>
    fun registerApp()
    fun unregisterApp()
}

internal class UnifiedPushImpl(private val context: Context) : app.dapk.st.push.unifiedpush.UnifiedPush {
    override fun saveDistributor(distributor: String) = UnifiedPush.saveDistributor(context, distributor)
    override fun getDistributor(): String = UnifiedPush.getDistributor(context)
    override fun getDistributors(): List<String> = UnifiedPush.getDistributors(context)
    override fun registerApp() = UnifiedPush.registerApp(context)
    override fun unregisterApp() = UnifiedPush.unregisterApp(context)
}