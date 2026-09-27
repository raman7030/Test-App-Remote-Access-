package com.example.model

enum class DeviceOwnerStatus {
    NOT_ADMIN,
    PROFILE_OWNER,
    DEVICE_OWNER,
    ENROLLMENT_PENDING,
    RESTRICTED_OR_UNSUPPORTED
}

enum class ComplianceState {
    COMPLIANT,
    WARNING,
    NON_COMPLIANT,
    ACTION_REQUIRED
}

enum class AdminRole {
    SUPER_ADMIN,
    DEVICE_ADMIN,
    REMOTE_SUPPORT_OPERATOR,
    READ_ONLY_AUDITOR
}

enum class CommandStatus {
    PENDING,
    SENT,
    EXECUTED,
    FAILED,
    EXPIRED
}

enum class CommandType {
    LOCK_DEVICE,
    REBOOT_DEVICE,
    APPLY_POLICY,
    SET_LOCK_MESSAGE,
    REQUEST_SUPPORT_SESSION,
    END_SUPPORT_SESSION,
    DISALLOW_CAMERA,
    ALLOW_CAMERA,
    DISALLOW_SCREEN_CAPTURE,
    ALLOW_SCREEN_CAPTURE,
    DISALLOW_USB_TRANSFER,
    ALLOW_USB_TRANSFER,
    DISALLOW_FACTORY_RESET,
    ALLOW_FACTORY_RESET,
    DISALLOW_SAFE_BOOT,
    ALLOW_SAFE_BOOT,
    SET_KIOSK_MODE,
    EXIT_KIOSK_MODE,
    REFRESH_INVENTORY,
    ENTERPRISE_WIPE
}

enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class DeviceTelemetry(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val ipAddress: String = "127.0.0.1",
    val networkType: String = "Wi-Fi",
    val signalStrength: Int = 4, // 0-4
    val osVersion: String = "Android 15",
    val apiLevel: Int = 35,
    val securityPatch: String = "2026-09-01",
    val manufacturer: String = "Google",
    val model: String = "Pixel 9 Pro",
    val serialNumber: String = "DCU-ENT-9921",
    val isEncrypted: Boolean = true,
    val isBiometricEnrolled: Boolean = true
)

data class ManagedPolicy(
    val key: String,
    val title: String,
    val description: String,
    val category: String, // "Security", "Hardware", "Applications", "Restrictions"
    val isEnabled: Boolean,
    val isSupportedOnDevice: Boolean,
    val isEnforced: Boolean,
    val failureReason: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ManagedDevice(
    val id: String,
    val name: String,
    val organizationId: String,
    val organizationName: String,
    val enrollmentStatus: DeviceOwnerStatus,
    val isOnline: Boolean,
    val lastSeenTimestamp: Long,
    val enrolledTimestamp: Long,
    val complianceState: ComplianceState,
    val telemetry: DeviceTelemetry,
    val activePoliciesCount: Int,
    val assignedAdmin: String,
    val deviceGroup: String,
    val isScreenSharingActive: Boolean = false,
    val isAccessibilityInteractionActive: Boolean = false,
    val activeSessionId: String? = null
)

data class ManagementCommand(
    val id: String,
    val deviceId: String,
    val organizationId: String,
    val adminId: String,
    val adminName: String,
    val type: CommandType,
    val parametersJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val status: CommandStatus = CommandStatus.PENDING,
    val signature: String = "",
    val executedAt: Long? = null,
    val executionDetails: String? = null
)

data class AuditLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actorEmail: String,
    val actorRole: AdminRole,
    val action: String,
    val targetDeviceId: String?,
    val targetDeviceName: String?,
    val severity: AuditSeverity,
    val details: String
)

data class RemoteGesture(
    val type: GestureType,
    val x: Float,
    val y: Float,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val textPayload: String? = null,
    val actionCode: Int = 0 // Global action code
)

enum class GestureType {
    TAP,
    LONG_PRESS,
    SWIPE,
    GLOBAL_ACTION,
    TEXT_INPUT
}
