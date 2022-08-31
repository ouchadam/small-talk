package app.dapk.st.firebase.messaging

import android.content.Context
import app.dapk.st.core.ProvidableModule
import app.dapk.st.core.extensions.unsafeLazy
import com.google.firebase.messaging.FirebaseMessaging

class MessagingModule(
    val serviceDelegate: ServiceDelegate,
    val context: Context,
) : ProvidableModule {

    val messaging by unsafeLazy {
        Messaging(FirebaseMessaging.getInstance(), context)
    }

}
