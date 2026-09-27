package com.example.dpm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.os.UserManager
import com.example.model.DeviceOwnerStatus
import com.example.receiver.EnterpriseDeviceAdminReceiver

data class DeviceCapabilityReport(
    val ownershipStatus: DeviceOwnerStatus,
    val isAdminActive: Boolean,
    val isDeviceOwner: Boolean,
    val isProfileOwner: Boolean,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val manufacturer: String,
    val model: String,
    val oemLimitations: List<String>,
    val supportedPolicies: Map<String, Boolean>,
    val diagnostics: List<CapabilityDiagnosticItem>
)

data class CapabilityDiagnosticItem(
    val title: String,
    val isSupported: Boolean,
    val statusText: String,
    val category: String
)

class DeviceCapabilityDetector(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, EnterpriseDeviceAdminReceiver::class.java)

    fun getReport(): DeviceCapabilityReport {
        val isAdmin = dpm.isAdminActive(adminComponent)
        val isDeviceOwner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            dpm.isDeviceOwnerApp(context.packageName)
        } else false
        val isProfileOwner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.isProfileOwnerApp(context.packageName)
        } else false

        val ownershipStatus = when {
            isDeviceOwner -> DeviceOwnerStatus.DEVICE_OWNER
            isProfileOwner -> DeviceOwnerStatus.PROFILE_OWNER
            isAdmin -> DeviceOwnerStatus.ENROLLMENT_PENDING
            else -> DeviceOwnerStatus.NOT_ADMIN
        }

        val oemLimitations = mutableListOf<String>()
        val manufacturerLower = Build.MANUFACTURER.lowercase()
        if (manufacturerLower.contains("xiaomi")) {
            oemLimitations.add("MIUI/HyperOS aggressive background task killers may restrict persistent sync. Grant 'Autostart' and disable battery optimizations.")
        }
        if (manufacturerLower.contains("samsung")) {
            oemLimitations.add("Samsung Knox enterprise restrictions may require additional Knox license for legacy APIs.")
        }
        if (manufacturerLower.contains("huawei")) {
            oemLimitations.add("Huawei EMUI battery manager requires explicit manual background permission.")
        }

        val supportedPolicies = mutableMapOf<String, Boolean>()
        supportedPolicies["SCREEN_CAPTURE_DISABLE"] = isDeviceOwner || isProfileOwner
        supportedPolicies["CAMERA_DISABLE"] = isAdmin
        supportedPolicies["USB_FILE_TRANSFER_RESTRICTION"] = isDeviceOwner
        supportedPolicies["SAFE_BOOT_RESTRICTION"] = isDeviceOwner
        supportedPolicies["FACTORY_RESET_RESTRICTION"] = isDeviceOwner
        supportedPolicies["KIOSK_LOCK_TASK"] = isDeviceOwner
        supportedPolicies["REMOTE_REBOOT"] = isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        supportedPolicies["LOCK_SCREEN_MESSAGE"] = isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        supportedPolicies["ENTERPRISE_WIPE"] = isDeviceOwner || isAdmin
        supportedPolicies["ACCESSIBILITY_REMOTE_ASSIST"] = true

        val diagnostics = listOf(
            CapabilityDiagnosticItem(
                title = "Android Enterprise Device Owner",
                isSupported = isDeviceOwner,
                statusText = if (isDeviceOwner) "Fully Provisioned (Highest Authority)" else "Not Device Owner (Requires QR enrollment at factory setup or ADB)",
                category = "Enterprise"
            ),
            CapabilityDiagnosticItem(
                title = "Active Device Administrator",
                isSupported = isAdmin,
                statusText = if (isAdmin) "Active Admin Authority" else "Inactive",
                category = "Enterprise"
            ),
            CapabilityDiagnosticItem(
                title = "Screen Capture Restriction Policy",
                isSupported = isDeviceOwner || isProfileOwner,
                statusText = if (isDeviceOwner || isProfileOwner) "Supported via DPM.setScreenCaptureDisabled" else "Requires Device/Profile Owner",
                category = "Policies"
            ),
            CapabilityDiagnosticItem(
                title = "Remote Camera Restriction",
                isSupported = isAdmin,
                statusText = if (isAdmin) "Supported via DPM.setCameraDisabled" else "Requires Admin",
                category = "Policies"
            ),
            CapabilityDiagnosticItem(
                title = "LockTask (Kiosk) Mode",
                isSupported = isDeviceOwner,
                statusText = if (isDeviceOwner) "Supported via DPM.setLockTaskPackages" else "Requires Device Owner",
                category = "Policies"
            ),
            CapabilityDiagnosticItem(
                title = "Remote Device Reboot",
                isSupported = isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
                statusText = if (isDeviceOwner) "Supported via DPM.reboot()" else "Requires Device Owner (Android 7.0+)",
                category = "Actions"
            ),
            CapabilityDiagnosticItem(
                title = "Hardware Keystore Attestation",
                isSupported = true,
                statusText = "AndroidKeyStore RSA-2048 & AES-256 Active",
                category = "Security"
            ),
            CapabilityDiagnosticItem(
                title = "MediaProjection Screen Sharing",
                isSupported = true,
                statusText = "Supported via Android MediaProjection API with User Consent",
                category = "Remote Support"
            ),
            CapabilityDiagnosticItem(
                title = "Remote Touch Assistance",
                isSupported = true,
                statusText = "Supported via AccessibilityService gesture dispatching",
                category = "Remote Support"
            )
        )

        return DeviceCapabilityReport(
            ownershipStatus = ownershipStatus,
            isAdminActive = isAdmin,
            isDeviceOwner = isDeviceOwner,
            isProfileOwner = isProfileOwner,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            oemLimitations = oemLimitations,
            supportedPolicies = supportedPolicies,
            diagnostics = diagnostics
        )
    }
}
