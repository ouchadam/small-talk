package app.dapk.st.directory

import android.content.Context
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.dapk.st.engine.RoomOverview
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.messenger.MessengerActivity

internal class ShortcutHandler(
    private val context: Context,
    private val iconLoader: IconLoader,
) {

    private val cachedRoomIds = mutableListOf<RoomId>()

    suspend fun onDirectoryUpdate(overviews: List<RoomOverview>) {
        val update = overviews.map { it.roomId }
        if (cachedRoomIds != update) {
            cachedRoomIds.clear()
            cachedRoomIds.addAll(update)

            val maxShortcutCountPerActivity = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            overviews
                .sortedByDescending { it.lastMessage?.utcTimestamp }
                .take(maxShortcutCountPerActivity)
                .forEachIndexed { index, room ->
                    val build = ShortcutInfoCompat.Builder(context, room.roomId.value)
                        .setShortLabel(room.roomName ?: "N/A")
                        .setLongLabel(room.roomName ?: "N/A")
                        .setRank(index)
                        .setLocusId((LocusIdCompat(room.roomId.value)))
                        .run {
                            this.setPerson(
                                Person.Builder()
                                    .setName(room.roomName ?: "N/A")
                                    .setKey(room.roomId.value)
                                    .build()
                            )
                        }
                        .run {
                            room.roomAvatarUrl?.let { iconLoader.load(it.value) }?.let {
                                this.setIcon(IconCompat.createFromIcon(context, it))
                            } ?: this
                        }
                        .setIntent(MessengerActivity.newShortcutInstance(context, room.roomId))
                        .setLongLived(true)
                        .setIsConversation()
                        .build()
                    ShortcutManagerCompat.pushDynamicShortcut(context, build)
                }
        }
    }

}