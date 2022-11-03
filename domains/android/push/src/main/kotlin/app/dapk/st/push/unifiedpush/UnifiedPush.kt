package app.dapk.st.push.unifiedpush

import android.content.Context
import org.unifiedpush.android.connector.UnifiedPush

interface UnifiedPush {
    fun saveDistributor(context: Context, distributor: String) = UnifiedPush.saveDistributor(context, distributor)
    fun getDistributor(context: Context): String = UnifiedPush.getDistributor(context)
    fun getDistributors(context: Context): List<String> = UnifiedPush.getDistributors(context)
    fun registerApp(context: Context) = UnifiedPush.registerApp(context)
    fun unregisterApp(context: Context) = UnifiedPush.unregisterApp(context)
}