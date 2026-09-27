package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CommandStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    fun getDeviceById(deviceId: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceByIdDirect(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("UPDATE devices SET isOnline = :isOnline, lastSeenTimestamp = :lastSeen WHERE id = :deviceId")
    suspend fun updateOnlineStatus(deviceId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE devices SET isScreenSharingActive = :isActive, activeSessionId = :sessionId WHERE id = :deviceId")
    suspend fun updateScreenSharingStatus(deviceId: String, isActive: Boolean, sessionId: String?)
}

@Dao
interface PolicyDao {
    @Query("SELECT * FROM policies WHERE deviceId = :deviceId")
    fun getPoliciesForDevice(deviceId: String): Flow<List<PolicyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicies(policies: List<PolicyEntity>)

    @Query("UPDATE policies SET isEnabled = :isEnabled, isEnforced = :isEnforced, lastUpdated = :timestamp, failureReason = :failureReason WHERE id = :policyId")
    suspend fun updatePolicyStatus(policyId: String, isEnabled: Boolean, isEnforced: Boolean, timestamp: Long, failureReason: String?)
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getCommandsForDevice(deviceId: String): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands WHERE deviceId = :deviceId AND status = :status ORDER BY timestamp ASC")
    suspend fun getPendingCommands(deviceId: String, status: CommandStatus = CommandStatus.PENDING): List<CommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandEntity)

    @Query("UPDATE commands SET status = :status, executedAt = :executedAt, executionDetails = :details WHERE id = :commandId")
    suspend fun updateCommandStatus(commandId: String, status: CommandStatus, executedAt: Long, details: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE targetDeviceId = :deviceId ORDER BY timestamp DESC")
    fun getAuditLogsForDevice(deviceId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface AdminUserDao {
    @Query("SELECT * FROM admin_users ORDER BY displayName ASC")
    fun getAllAdmins(): Flow<List<AdminUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: AdminUserEntity)
}
