package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AdminRole
import com.example.model.AuditSeverity
import com.example.model.CommandStatus
import com.example.model.CommandType
import com.example.model.ComplianceState
import com.example.model.DeviceOwnerStatus

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val organizationId: String,
    val organizationName: String,
    val enrollmentStatus: DeviceOwnerStatus,
    val isOnline: Boolean,
    val lastSeenTimestamp: Long,
    val enrolledTimestamp: Long,
    val complianceState: ComplianceState,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val ipAddress: String,
    val networkType: String,
    val signalStrength: Int,
    val osVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
    val isEncrypted: Boolean,
    val isBiometricEnrolled: Boolean,
    val activePoliciesCount: Int,
    val assignedAdmin: String,
    val deviceGroup: String,
    val isScreenSharingActive: Boolean,
    val isAccessibilityInteractionActive: Boolean,
    val activeSessionId: String?
)

@Entity(tableName = "policies")
data class PolicyEntity(
    @PrimaryKey val id: String, // composite: deviceId_key
    val deviceId: String,
    val policyKey: String,
    val title: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean,
    val isSupportedOnDevice: Boolean,
    val isEnforced: Boolean,
    val failureReason: String?,
    val lastUpdated: Long
)

@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val organizationId: String,
    val adminId: String,
    val adminName: String,
    val type: CommandType,
    val parametersJson: String,
    val timestamp: Long,
    val expiresAt: Long,
    val status: CommandStatus,
    val signature: String,
    val executedAt: Long?,
    val executionDetails: String?
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val actorEmail: String,
    val actorRole: AdminRole,
    val action: String,
    val targetDeviceId: String?,
    val targetDeviceName: String?,
    val severity: AuditSeverity,
    val details: String
)

@Entity(tableName = "admin_users")
data class AdminUserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val role: AdminRole,
    val department: String,
    val lastLogin: Long,
    val isMfaEnabled: Boolean
)
