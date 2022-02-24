package fixture

import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId

fun aRoomMember(
    id: UserId = aUserId(),
    displayName: String? = null,
    avatarUrl: AvatarUrl? = null
) = RoomMember(id, displayName, avatarUrl)