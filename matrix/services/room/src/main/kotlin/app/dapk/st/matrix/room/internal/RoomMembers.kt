package app.dapk.st.matrix.room.internal

import app.dapk.st.core.LRUCache
import app.dapk.st.core.isNullOrEmpty
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.room.MemberStore

class RoomMembers(private val memberStore: MemberStore, private val membersCache: RoomMembersCache) {

    suspend fun findMember(roomId: RoomId, userId: UserId): RoomMember? {
        return findMembers(roomId, listOf(userId)).firstOrNull()
    }

    suspend fun findMembers(roomId: RoomId, userIds: List<UserId>): List<RoomMember> {
        val roomCache = membersCache.room(roomId)

        return if (roomCache.isNullOrEmpty()) {
            memberStore.query(roomId, userIds).also { membersCache.insert(roomId, it) }
        } else {
            val (cachedMembers, missingIds) = userIds.fold(mutableListOf<RoomMember>() to mutableListOf<UserId>()) { acc, current ->
                when (val member = roomCache?.get(current)) {
                    null -> acc.second.add(current)
                    else -> acc.first.add(member)
                }
                acc
            }

            when {
                missingIds.isNotEmpty() -> {
                    (memberStore.query(roomId, missingIds).also { membersCache.insert(roomId, it) } + cachedMembers)
                }

                else -> cachedMembers
            }
        }
    }

    suspend fun findMembersSummary(roomId: RoomId) = memberStore.query(roomId, limit = 8)

    suspend fun insert(roomId: RoomId, members: List<RoomMember>) {
        membersCache.insert(roomId, members)
        memberStore.insert(roomId, members)
    }
}

private const val ROOMS_TO_CACHE_MEMBERS_FOR_SIZE = 12
private const val MEMBERS_TO_CACHE_PER_ROOM = 25

class RoomMembersCache {

    private val cache = LRUCache<RoomId, LRUCache<UserId, RoomMember>>(maxSize = ROOMS_TO_CACHE_MEMBERS_FOR_SIZE)

    fun room(roomId: RoomId) = cache.get(roomId)

    fun insert(roomId: RoomId, members: List<RoomMember>) {
        val map = cache.getOrPut(roomId) { LRUCache(maxSize = MEMBERS_TO_CACHE_PER_ROOM) }
        members.forEach { map.put(it.id, it) }
    }
}
