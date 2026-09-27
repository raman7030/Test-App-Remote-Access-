package com.example.dpm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.example.model.ManagedPolicy
import com.example.receiver.EnterpriseDeviceAdminReceiver

class EnterprisePolicyManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, EnterpriseDeviceAdminReceiver::class.java)

    fun getAdminComponent(): ComponentName = adminComponent

    fun isAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    fun isDeviceOwner(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        dpm.isDeviceOwnerApp(context.packageName)
    } else false

    fun isProfileOwner(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        dpm.isProfileOwnerApp(context.packageName)
    } else false

    fun setCameraDisabled(disabled: Boolean): Result<Unit> = runCatching {
        if (!isAdminActive()) throw SecurityException("App is not an active Device Admin")
        dpm.setCameraDisabled(adminComponent, disabled)
    }

    fun isCameraDisabled(): Boolean = runCatching {
        dpm.getCameraDisabled(adminComponent)
    }.getOrDefault(false)

    fun setScreenCaptureDisabled(disabled: Boolean): Result<Unit> = runCatching {
        if (!isDeviceOwner() && !isProfileOwner()) throw SecurityException("Requires Device Owner or Profile Owner")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.setScreenCaptureDisabled(adminComponent, disabled)
        }
    }

    fun isScreenCaptureDisabled(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.getScreenCaptureDisabled(adminComponent)
        } else false
    }.getOrDefault(false)

    fun setUserRestriction(restriction: String, enabled: Boolean): Result<Unit> = runCatching {
        if (!isDeviceOwner() && !isProfileOwner()) throw SecurityException("Requires Device Owner or Profile Owner authority")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (enabled) {
                dpm.addUserRestriction(adminComponent, restriction)
            } else {
                dpm.clearUserRestriction(adminComponent, restriction)
            }
        }
    }

    fun isUserRestrictionEnforced(restriction: String): Boolean = runCatching {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bundle = dpm.getUserRestrictions(adminComponent)
            bundle.getBoolean(restriction, false) || userManager.hasUserRestriction(restriction)
        } else false
    }.getOrDefault(false)

    fun lockNow(): Result<Unit> = runCatching {
        if (!isAdminActive()) throw SecurityException("App is not an active Device Admin")
        dpm.lockNow()
    }

    fun reboot(): Result<Unit> = runCatching {
        if (!isDeviceOwner()) throw SecurityException("Reboot requires Device Owner status")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dpm.reboot(adminComponent)
        } else {
            throw UnsupportedOperationException("Reboot requires Android 7.0 (API 24)+")
        }
    }

    fun setLockScreenMessage(message: String): Result<Unit> = runCatching {
        if (!isDeviceOwner()) throw SecurityException("Requires Device Owner status")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dpm.setDeviceOwnerLockScreenInfo(adminComponent, message)
        }
    }

    fun setLockTaskPackages(packages: Array<String>): Result<Unit> = runCatching {
        if (!isDeviceOwner()) throw SecurityException("Requires Device Owner status")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.setLockTaskPackages(adminComponent, packages)
        }
    }

    fun isLockTaskPermitted(packageName: String): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.isLockTaskPermitted(packageName)
        } else false
    }.getOrDefault(false)

    fun wipeData(): Result<Unit> = runCatching {
        if (!isAdminActive()) throw SecurityException("Requires Device Admin authority")
        dpm.wipeData(0)
    }

    fun getStandardPoliciesList(): List<ManagedPolicy> {
        val isDo = isDeviceOwner()
        val isAdmin = isAdminActive()

        return listOf(
            ManagedPolicy(
                key = "camera_disabled",
                title = "Disable Device Camera",
                description = "Prevents hardware camera access across all apps",
                category = "Hardware",
                isEnabled = isCameraDisabled(),
                isSupportedOnDevice = isAdmin,
                isEnforced = isCameraDisabled(),
                failureReason = if (!isAdmin) "Requires active Device Admin" else null
            ),
            ManagedPolicy(
                key = "screen_capture_disabled",
                title = "Disallow Screen Capture",
                description = "Blocks screenshots, screen recording, and unauthorized projection",
                category = "Security",
                isEnabled = isScreenCaptureDisabled(),
                isSupportedOnDevice = isDo,
                isEnforced = isScreenCaptureDisabled(),
                failureReason = if (!isDo) "Requires Device Owner" else null
            ),
            ManagedPolicy(
                key = "usb_transfer_disabled",
                title = "Block USB File Transfer",
                description = "Disallows MTP and PTP USB storage connections to PC",
                category = "Restrictions",
                isEnabled = isUserRestrictionEnforced(UserManager.DISALLOW_USB_FILE_TRANSFER),
                isSupportedOnDevice = isDo,
                isEnforced = isUserRestrictionEnforced(UserManager.DISALLOW_USB_FILE_TRANSFER),
                failureReason = if (!isDo) "Requires Device Owner" else null
            ),
            ManagedPolicy(
                key = "factory_reset_disabled",
                title = "Block Factory Reset",
                description = "Prevents device user from executing a factory data reset in Settings",
                category = "Security",
                isEnabled = isUserRestrictionEnforced(UserManager.DISALLOW_FACTORY_RESET),
                isSupportedOnDevice = isDo,
                isEnforced = isUserRestrictionEnforced(UserManager.DISALLOW_FACTORY_RESET),
                failureReason = if (!isDo) "Requires Device Owner" else null
            ),
            ManagedPolicy(
                key = "safe_boot_disabled",
                title = "Disallow Safe Boot Mode",
                description = "Prevents rebooting the device into Android safe mode to bypass MDM",
                category = "Security",
                isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    isUserRestrictionEnforced(UserManager.DISALLOW_SAFE_BOOT)
                } else false,
                isSupportedOnDevice = isDo,
                isEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    isUserRestrictionEnforced(UserManager.DISALLOW_SAFE_BOOT)
                } else false,
                failureReason = if (!isDo) "Requires Device Owner" else null
            ),
            ManagedPolicy(
                key = "unknown_sources_disabled",
                title = "Disallow Unknown Source Installs",
                description = "Restricts sideloading APKs outside of managed Google Play",
                category = "Applications",
                isEnabled = isUserRestrictionEnforced(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES),
                isSupportedOnDevice = isDo,
                isEnforced = isUserRestrictionEnforced(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES),
                failureReason = if (!isDo) "Requires Device Owner" else null
            ),
            ManagedPolicy(
                key = "kiosk_lock_task",
                title = "Kiosk LockTask Allowlist",
                description = "Enables dedicated single-purpose kiosk lock-task pin",
                category = "Restrictions",
                isEnabled = isLockTaskPermitted(context.packageName),
                isSupportedOnDevice = isDo,
                isEnforced = isLockTaskPermitted(context.packageName),
                failureReason = if (!isDo) "Requires Device Owner" else null
            )
        )
    }
}
