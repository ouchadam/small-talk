package app.dapk.st.matrix.room.internal

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.MemberStore

class RoomMembers(private val memberStore: MemberStore) {

    private val cache = mutableMapOf<RoomId, MutableMap<UserId, RoomMember>>()

    suspend fun findMember(roomId: RoomId, userId: UserId): RoomMember? {
        return findMembers(roomId, listOf(userId)).firstOrNull()
    }

    suspend fun findMembers(roomId: RoomId, userIds: List<UserId>): List<RoomMember> {
        val roomCache = cache[roomId]

        return if (roomCache.isNullOrEmpty()) {
            memberStore.query(roomId, userIds).also { cache(roomId, it) }
        } else {
            val (cachedMembers, missingIds) = userIds.fold(mutableListOf<RoomMember>() to mutableListOf<UserId>()) { acc, current ->
                when (val member = roomCache[current]) {
                    null -> acc.second.add(current)
                    else -> acc.first.add(member)
                }
                acc
            }

            when {
                missingIds.isNotEmpty() -> {
                    (memberStore.query(roomId, missingIds).also { cache(roomId, it) } + cachedMembers)
                }
                else -> cachedMembers
            }
        }
    }

    suspend fun insert(roomId: RoomId, members: List<RoomMember>) {
        cache(roomId, members)
        memberStore.insert(roomId, members)
    }

    private fun cache(roomId: RoomId, members: List<RoomMember>) {
        val map = cache.getOrPut(roomId) { mutableMapOf() }
        members.forEach { map[it.id] = it }
    }

}