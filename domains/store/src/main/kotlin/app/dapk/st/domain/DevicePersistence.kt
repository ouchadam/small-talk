package app.dapk.st.domain

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.db.DapkDb
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.SessionId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.device.KnownDeviceStore
import app.dapk.st.matrix.device.internal.DeviceKeys
import kotlinx.serialization.json.Json

class DevicePersistence(
    private val database: DapkDb,
    private val devicesCache: KnownDevicesCache,
    private val dispatchers: CoroutineDispatchers,
) : KnownDeviceStore {

    override suspend fun associateSession(sessionId: SessionId, deviceIds: List<DeviceId>) {
        dispatchers.withIoContext {
            database.deviceQueries.transaction {
                deviceIds.forEach {
                    database.deviceQueries.insertDeviceToMegolmSession(
                        device_id = it.value,
                        session_id = sessionId.value
                    )
                }
            }
        }
    }

    override suspend fun markOutdated(userIds: List<UserId>) {
        devicesCache.updateOutdated(userIds)
        database.deviceQueries.markOutdated(userIds.map { it.value })
    }

    override suspend fun maybeConsumeOutdated(userIds: List<UserId>): List<UserId> {
        return devicesCache.consumeOutdated(userIds).also {
            database.deviceQueries.markIndate(userIds.map { it.value })
        }
    }

    override suspend fun updateDevices(devices: Map<UserId, Map<DeviceId, DeviceKeys>>): List<DeviceKeys> {
        devicesCache.putAll(devices)
        database.deviceQueries.transaction {
            devices.forEach { (userId, innerMap) ->
                innerMap.forEach { (deviceId, keys) ->
                    database.deviceQueries.insertDevice(
                        user_id = userId.value,
                        device_id = deviceId.value,
                        blob = Json.encodeToString(DeviceKeys.serializer(), keys),
                    )
                }
            }
        }
        return devicesCache.devices()
    }

    override suspend fun devicesMegolmSession(userIds: List<UserId>, sessionId: SessionId): List<DeviceKeys> {
        return database.deviceQueries.selectUserDevicesWithSessions(userIds.map { it.value }, sessionId.value).executeAsList().map {
            Json.decodeFromString(DeviceKeys.serializer(), it.blob)
        }
    }

    override suspend fun device(userId: UserId, deviceId: DeviceId): DeviceKeys? {
        return devicesCache.device(userId, deviceId) ?: database.deviceQueries.selectDevice(deviceId.value).executeAsOneOrNull()?.let {
            Json.decodeFromString(DeviceKeys.serializer(), it)
        }?.also { devicesCache.putAll(mapOf(userId to mapOf(deviceId to it))) }
    }
}

class KnownDevicesCache(
    private val devicesCache: Map<UserId, MutableMap<DeviceId, DeviceKeys>> = mutableMapOf(),
    private var outdatedUserIds: MutableSet<UserId> = mutableSetOf()
) {

    fun consumeOutdated(userIds: List<UserId>): List<UserId> {
        val outdatedToConsume = outdatedUserIds.filter { userIds.contains(it) }
//        val unknownIds = userIds.filter { devicesCache[it] == null }
        outdatedUserIds = (outdatedUserIds - outdatedToConsume.toSet()).toMutableSet()
        return outdatedToConsume
    }

    fun updateOutdated(userIds: List<UserId>) {
        outdatedUserIds.addAll(userIds)
    }

    fun putAll(devices: Map<UserId, Map<DeviceId, DeviceKeys>>) {
        devices.mapValues { it.value.toMutableMap() }
    }

    fun devices(): List<DeviceKeys> {
        return devicesCache.values.map { it.values }.flatten()
    }

    fun device(userId: UserId, deviceId: DeviceId): DeviceKeys? {
        return devicesCache[userId]?.get(deviceId)
    }
}