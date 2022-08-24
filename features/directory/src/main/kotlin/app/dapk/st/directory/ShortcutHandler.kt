package app.dapk.st.directory

import android.content.Context
import android.content.pm.ShortcutInfo
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomOverview
import app.dapk.st.messenger.MessengerActivity

class ShortcutHandler(private val context: Context) {

    private val cachedRoomIds = mutableListOf<RoomId>()

    fun onDirectoryUpdate(overviews: List<RoomOverview>) {
        val update = overviews.map { it.roomId }
        if (cachedRoomIds != update) {
            cachedRoomIds.clear()
            cachedRoomIds.addAll(update)

            val maxShortcutCountPerActivity = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            overviews
                .take(maxShortcutCountPerActivity)
                .forEachIndexed { index, room ->
                    val build = ShortcutInfoCompat.Builder(context, room.roomId.value)
                        .setShortLabel(room.roomName ?: "N/A")
                        .setLongLabel(room.roomName ?: "N/A")
                        .setRank(index)
                        .run {
                            this.setPerson(
                                Person.Builder()
                                    .setName(room.roomName ?: "N/A")
                                    .setKey(room.roomId.value)
                                    .build()
                            )
                        }
                        .setIntent(MessengerActivity.newShortcutInstance(context, room.roomId))
                        .setLongLived(true)
                        .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
                        .build()
                    ShortcutManagerCompat.pushDynamicShortcut(context, build)
                }
        }
    }

}