package com.example.repository

import android.content.Context
import com.example.data.local.AdminUserEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.CommandEntity
import com.example.data.local.DeviceEntity
import com.example.data.local.PolicyEntity
import com.example.dpm.DeviceCapabilityDetector
import com.example.dpm.EnterprisePolicyManager
import com.example.model.AdminRole
import com.example.model.AuditLog
import com.example.model.AuditSeverity
import com.example.model.CommandStatus
import com.example.model.CommandType
import com.example.model.ComplianceState
import com.example.model.DeviceOwnerStatus
import com.example.model.DeviceTelemetry
import com.example.model.ManagedDevice
import com.example.model.ManagedPolicy
import com.example.model.ManagementCommand
import com.example.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EnterpriseRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val keystoreManager: KeystoreManager,
    private val policyManager: EnterprisePolicyManager,
    private val capabilityDetector: DeviceCapabilityDetector
) {

    val allDevices: Flow<List<ManagedDevice>> = database.deviceDao().getAllDevices().map { entities ->
        entities.map { it.toModel() }
    }

    val auditLogs: Flow<List<AuditLog>> = database.auditLogDao().getAuditLogs().map { entities ->
        entities.map { it.toModel() }
    }

    val adminUsers: Flow<List<AdminUserEntity>> = database.adminUserDao().getAllAdmins()

    fun getDeviceById(deviceId: String): Flow<ManagedDevice?> =
        database.deviceDao().getDeviceById(deviceId).map { it?.toModel() }

    fun getPoliciesForDevice(deviceId: String): Flow<List<ManagedPolicy>> =
        database.policyDao().getPoliciesForDevice(deviceId).map { entities ->
            entities.map { it.toModel() }
        }

    fun getCommandsForDevice(deviceId: String): Flow<List<ManagementCommand>> =
        database.commandDao().getCommandsForDevice(deviceId).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun sendManagementCommand(
        deviceId: String,
        type: CommandType,
        parametersJson: String = "{}",
        adminRole: AdminRole = AdminRole.SUPER_ADMIN,
        adminName: String = "Sarah Jenkins"
    ): Result<String> {
        return runCatching {
            val commandId = "CMD-" + UUID.randomUUID().toString().take(8).uppercase()
            val rawPayload = "$commandId:$deviceId:$type:$parametersJson:${System.currentTimeMillis()}"
            val signature = keystoreManager.signData(rawPayload.toByteArray(Charsets.UTF_8))

            val commandEntity = CommandEntity(
                id = commandId,
                deviceId = deviceId,
                organizationId = "ORG-DROIDCOMMAND-GLOBAL",
                adminId = "ADM-001",
                adminName = adminName,
                type = type,
                parametersJson = parametersJson,
                timestamp = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 86400000L,
                status = CommandStatus.PENDING,
                signature = signature,
                executedAt = null,
                executionDetails = null
            )
            database.commandDao().insertCommand(commandEntity)

            // Log audit event
            insertAuditLog(
                action = "DISPATCH_COMMAND",
                actorRole = adminRole,
                targetDeviceId = deviceId,
                severity = if (type == CommandType.ENTERPRISE_WIPE || type == CommandType.REBOOT_DEVICE) AuditSeverity.CRITICAL else AuditSeverity.INFO,
                details = "Admin dispatched command: $type (ID: $commandId)"
            )

            // If command targets the local device, execute immediately
            val localDeviceId = keystoreManager.getDeviceUuid()
            if (deviceId == localDeviceId || deviceId == "LOCAL_HOST_DEVICE") {
                executeCommandLocally(commandEntity)
            }

            commandId
        }
    }

    private suspend fun executeCommandLocally(cmd: CommandEntity) {
        var detail = "Executed"
        var status = CommandStatus.EXECUTED
        when (cmd.type) {
            CommandType.LOCK_DEVICE -> {
                policyManager.lockNow().onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Failed"
                }
            }
            CommandType.REBOOT_DEVICE -> {
                policyManager.reboot().onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Reboot requires Device Owner"
                }
            }
            CommandType.DISALLOW_CAMERA -> {
                policyManager.setCameraDisabled(true).onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Failed"
                }
            }
            CommandType.ALLOW_CAMERA -> {
                policyManager.setCameraDisabled(false).onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Failed"
                }
            }
            CommandType.DISALLOW_SCREEN_CAPTURE -> {
                policyManager.setScreenCaptureDisabled(true).onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Failed"
                }
            }
            CommandType.ALLOW_SCREEN_CAPTURE -> {
                policyManager.setScreenCaptureDisabled(false).onFailure {
                    status = CommandStatus.FAILED
                    detail = it.localizedMessage ?: "Failed"
                }
            }
            else -> {
                detail = "Executed via enterprise policy manager"
            }
        }

        database.commandDao().updateCommandStatus(
            commandId = cmd.id,
            status = status,
            executedAt = System.currentTimeMillis(),
            details = detail
        )
    }

    suspend fun toggleDevicePolicy(deviceId: String, policyKey: String, enable: Boolean): Result<Unit> {
        return runCatching {
            var failureReason: String? = null
            var enforced = enable

            val localDeviceId = keystoreManager.getDeviceUuid()
            if (deviceId == localDeviceId || deviceId == "LOCAL_HOST_DEVICE") {
                when (policyKey) {
                    "camera_disabled" -> {
                        policyManager.setCameraDisabled(enable).onFailure {
                            failureReason = it.localizedMessage
                            enforced = false
                        }
                    }
                    "screen_capture_disabled" -> {
                        policyManager.setScreenCaptureDisabled(enable).onFailure {
                            failureReason = it.localizedMessage
                            enforced = false
                        }
                    }
                    "usb_transfer_disabled" -> {
                        policyManager.setUserRestriction(android.os.UserManager.DISALLOW_USB_FILE_TRANSFER, enable).onFailure {
                            failureReason = it.localizedMessage
                            enforced = false
                        }
                    }
                    "factory_reset_disabled" -> {
                        policyManager.setUserRestriction(android.os.UserManager.DISALLOW_FACTORY_RESET, enable).onFailure {
                            failureReason = it.localizedMessage
                            enforced = false
                        }
                    }
                    "safe_boot_disabled" -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            policyManager.setUserRestriction(android.os.UserManager.DISALLOW_SAFE_BOOT, enable).onFailure {
                                failureReason = it.localizedMessage
                                enforced = false
                            }
                        }
                    }
                    "unknown_sources_disabled" -> {
                        policyManager.setUserRestriction(android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, enable).onFailure {
                            failureReason = it.localizedMessage
                            enforced = false
                        }
                    }
                }
            }

            database.policyDao().updatePolicyStatus(
                policyId = "${deviceId}_${policyKey}",
                isEnabled = enable,
                isEnforced = enforced,
                timestamp = System.currentTimeMillis(),
                failureReason = failureReason
            )

            insertAuditLog(
                action = "POLICY_TOGGLED",
                actorRole = AdminRole.SUPER_ADMIN,
                targetDeviceId = deviceId,
                severity = AuditSeverity.WARNING,
                details = "Policy '$policyKey' updated to $enable (Enforced: $enforced)"
            )
        }
    }

    suspend fun updateScreenSharingStatus(deviceId: String, isActive: Boolean, sessionId: String?) {
        database.deviceDao().updateScreenSharingStatus(deviceId, isActive, sessionId)
    }

    suspend fun insertAuditLog(
        action: String,
        actorRole: AdminRole,
        targetDeviceId: String?,
        severity: AuditSeverity,
        details: String
    ) {
        database.auditLogDao().insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorEmail = "admin@droidcommand.ultimate",
                actorRole = actorRole,
                action = action,
                targetDeviceId = targetDeviceId,
                targetDeviceName = targetDeviceId,
                severity = severity,
                details = details
            )
        )
    }

    suspend fun seedInitialDataIfEmpty() {
        val count = database.deviceDao().getDeviceByIdDirect("LOCAL_HOST_DEVICE")
        if (count == null) {
            val localReport = capabilityDetector.getReport()
            val localId = keystoreManager.getDeviceUuid()

            // Seed Local device
            val localDevice = DeviceEntity(
                id = localId,
                name = "${localReport.manufacturer} ${localReport.model} (This Device)",
                organizationId = "ORG-DROIDCOMMAND-GLOBAL",
                organizationName = "HQ Engineering Fleet",
                enrollmentStatus = localReport.ownershipStatus,
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                enrolledTimestamp = System.currentTimeMillis() - 864000000L,
                complianceState = if (localReport.isDeviceOwner) ComplianceState.COMPLIANT else ComplianceState.ACTION_REQUIRED,
                batteryLevel = 92,
                isCharging = false,
                storageUsedBytes = 64_000_000_000L,
                storageTotalBytes = 128_000_000_000L,
                ramUsedMb = 3840,
                ramTotalMb = 8192,
                ipAddress = "192.168.1.105",
                networkType = "Wi-Fi (5GHz WPA3)",
                signalStrength = 4,
                osVersion = localReport.androidVersion,
                apiLevel = localReport.apiLevel,
                securityPatch = localReport.securityPatch,
                manufacturer = localReport.manufacturer,
                model = localReport.model,
                serialNumber = keystoreManager.getDeviceIdentityFingerprint(),
                isEncrypted = true,
                isBiometricEnrolled = true,
                activePoliciesCount = 4,
                assignedAdmin = "Sarah Jenkins (Super Admin)",
                deviceGroup = "Executive Android",
                isScreenSharingActive = false,
                isAccessibilityInteractionActive = false,
                activeSessionId = null
            )

            // Seed 2 enterprise fleet devices
            val remoteDevice1 = DeviceEntity(
                id = "DCU-FLEET-902",
                name = "Samsung Galaxy Tab S9 - Field Ops",
                organizationId = "ORG-DROIDCOMMAND-GLOBAL",
                organizationName = "Field Logistics Team",
                enrollmentStatus = DeviceOwnerStatus.DEVICE_OWNER,
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis() - 12000L,
                enrolledTimestamp = System.currentTimeMillis() - 2500000000L,
                complianceState = ComplianceState.COMPLIANT,
                batteryLevel = 78,
                isCharging = true,
                storageUsedBytes = 42_000_000_000L,
                storageTotalBytes = 256_000_000_000L,
                ramUsedMb = 4600,
                ramTotalMb = 12288,
                ipAddress = "10.240.18.91",
                networkType = "5G LTE Enterprise",
                signalStrength = 4,
                osVersion = "Android 14 (OneUI 6.1)",
                apiLevel = 34,
                securityPatch = "2026-08-01",
                manufacturer = "Samsung",
                model = "SM-X710",
                serialNumber = "R52N80Z19EK",
                isEncrypted = true,
                isBiometricEnrolled = true,
                activePoliciesCount = 6,
                assignedAdmin = "David Vance (Device Admin)",
                deviceGroup = "Field Tablets",
                isScreenSharingActive = false,
                isAccessibilityInteractionActive = false,
                activeSessionId = null
            )

            val remoteDevice2 = DeviceEntity(
                id = "DCU-FLEET-314",
                name = "Zebra TC58 Enterprise Scanner",
                organizationId = "ORG-DROIDCOMMAND-GLOBAL",
                organizationName = "Warehouse West Hub",
                enrollmentStatus = DeviceOwnerStatus.DEVICE_OWNER,
                isOnline = false,
                lastSeenTimestamp = System.currentTimeMillis() - 1800000L,
                enrolledTimestamp = System.currentTimeMillis() - 4000000000L,
                complianceState = ComplianceState.WARNING,
                batteryLevel = 19,
                isCharging = false,
                storageUsedBytes = 18_000_000_000L,
                storageTotalBytes = 64_000_000_000L,
                ramUsedMb = 2900,
                ramTotalMb = 4096,
                ipAddress = "10.240.55.12",
                networkType = "Wi-Fi (Warehouse-AP04)",
                signalStrength = 2,
                osVersion = "Android 13",
                apiLevel = 33,
                securityPatch = "2026-05-01",
                manufacturer = "Zebra Technologies",
                model = "TC58",
                serialNumber = "21295521400248",
                isEncrypted = true,
                isBiometricEnrolled = false,
                activePoliciesCount = 5,
                assignedAdmin = "Sarah Jenkins (Super Admin)",
                deviceGroup = "Logistics Scanners",
                isScreenSharingActive = false,
                isAccessibilityInteractionActive = false,
                activeSessionId = null
            )

            database.deviceDao().insertDevices(listOf(localDevice, remoteDevice1, remoteDevice2))

            // Seed policies for devices
            val standardPolicies = policyManager.getStandardPoliciesList()
            listOf(localId, remoteDevice1.id, remoteDevice2.id).forEach { devId ->
                val policies = standardPolicies.map { p ->
                    PolicyEntity(
                        id = "${devId}_${p.key}",
                        deviceId = devId,
                        policyKey = p.key,
                        title = p.title,
                        description = p.description,
                        category = p.category,
                        isEnabled = p.isEnabled,
                        isSupportedOnDevice = p.isSupportedOnDevice,
                        isEnforced = p.isEnforced,
                        failureReason = p.failureReason,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                database.policyDao().insertPolicies(policies)
            }

            // Seed Admins
            val admins = listOf(
                AdminUserEntity("ADM-001", "sarah.jenkins@enterprise.com", "Sarah Jenkins", AdminRole.SUPER_ADMIN, "Cybersecurity & MDM", System.currentTimeMillis(), true),
                AdminUserEntity("ADM-002", "david.vance@enterprise.com", "David Vance", AdminRole.DEVICE_ADMIN, "Field Operations", System.currentTimeMillis() - 7200000L, true),
                AdminUserEntity("ADM-003", "elena.rostova@enterprise.com", "Elena Rostova", AdminRole.REMOTE_SUPPORT_OPERATOR, "IT Helpdesk Tier 2", System.currentTimeMillis() - 86400000L, false),
                AdminUserEntity("ADM-004", "marcus.brody@enterprise.com", "Marcus Brody", AdminRole.READ_ONLY_AUDITOR, "Compliance & Risk", System.currentTimeMillis() - 3600000L, true)
            )
            admins.forEach { database.adminUserDao().insertAdmin(it) }

            // Seed initial Audit logs
            val logs = listOf(
                AuditLogEntity(UUID.randomUUID().toString(), System.currentTimeMillis() - 3600000L, "sarah.jenkins@enterprise.com", AdminRole.SUPER_ADMIN, "ENROLLMENT_TOKEN_CREATED", null, null, AuditSeverity.INFO, "Generated Android Enterprise zero-touch enrollment token: TKN-SECURE-9921-X"),
                AuditLogEntity(UUID.randomUUID().toString(), System.currentTimeMillis() - 1800000L, "system@enterprise.local", AdminRole.SUPER_ADMIN, "HEALTH_CHECK_COMPLIANT", remoteDevice1.id, remoteDevice1.name, AuditSeverity.INFO, "Device policy compliance check passed. All 6 policies active."),
                AuditLogEntity(UUID.randomUUID().toString(), System.currentTimeMillis() - 900000L, "david.vance@enterprise.com", AdminRole.DEVICE_ADMIN, "SCREEN_SHARE_SESSION_COMPLETED", remoteDevice1.id, remoteDevice1.name, AuditSeverity.INFO, "Remote support session completed normally. Duration: 6m 42s. Authorized touch: Yes.")
            )
            logs.forEach { database.auditLogDao().insertLog(it) }
        }
    }

    private fun DeviceEntity.toModel(): ManagedDevice {
        return ManagedDevice(
            id = id,
            name = name,
            organizationId = organizationId,
            organizationName = organizationName,
            enrollmentStatus = enrollmentStatus,
            isOnline = isOnline,
            lastSeenTimestamp = lastSeenTimestamp,
            enrolledTimestamp = enrolledTimestamp,
            complianceState = complianceState,
            telemetry = DeviceTelemetry(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                storageUsedBytes = storageUsedBytes,
                storageTotalBytes = storageTotalBytes,
                ramUsedMb = ramUsedMb,
                ramTotalMb = ramTotalMb,
                ipAddress = ipAddress,
                networkType = networkType,
                signalStrength = signalStrength,
                osVersion = osVersion,
                apiLevel = apiLevel,
                securityPatch = securityPatch,
                manufacturer = manufacturer,
                model = model,
                serialNumber = serialNumber,
                isEncrypted = isEncrypted,
                isBiometricEnrolled = isBiometricEnrolled
            ),
            activePoliciesCount = activePoliciesCount,
            assignedAdmin = assignedAdmin,
            deviceGroup = deviceGroup,
            isScreenSharingActive = isScreenSharingActive,
            isAccessibilityInteractionActive = isAccessibilityInteractionActive,
            activeSessionId = activeSessionId
        )
    }

    private fun PolicyEntity.toModel(): ManagedPolicy {
        return ManagedPolicy(
            key = policyKey,
            title = title,
            description = description,
            category = category,
            isEnabled = isEnabled,
            isSupportedOnDevice = isSupportedOnDevice,
            isEnforced = isEnforced,
            failureReason = failureReason,
            lastUpdated = lastUpdated
        )
    }

    private fun CommandEntity.toModel(): ManagementCommand {
        return ManagementCommand(
            id = id,
            deviceId = deviceId,
            organizationId = organizationId,
            adminId = adminId,
            adminName = adminName,
            type = type,
            parametersJson = parametersJson,
            timestamp = timestamp,
            expiresAt = expiresAt,
            status = status,
            signature = signature,
            executedAt = executedAt,
            executionDetails = executionDetails
        )
    }

    private fun AuditLogEntity.toModel(): AuditLog {
        return AuditLog(
            id = id,
            timestamp = timestamp,
            actorEmail = actorEmail,
            actorRole = actorRole,
            action = action,
            targetDeviceId = targetDeviceId,
            targetDeviceName = targetDeviceName,
            severity = severity,
            details = details
        )
    }
}
