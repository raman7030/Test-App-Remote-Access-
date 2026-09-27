package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.model.AdminRole
import com.example.model.AuditSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class EnterpriseDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        logEvent(context, "DEVICE_ADMIN_ENABLED", "Enterprise Device Admin enabled on device", AuditSeverity.INFO)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        logEvent(context, "DEVICE_ADMIN_DISABLED", "Device Admin disabled or revoked", AuditSeverity.WARNING)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        logEvent(context, "PROVISIONING_COMPLETE", "Android Enterprise Device Owner provisioning successfully completed", AuditSeverity.INFO)
        Toast.makeText(context, "DroidCommand Device Owner Provisioning Complete", Toast.LENGTH_LONG).show()
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        logEvent(context, "LOCK_TASK_ENTER", "Device entered LockTask (Kiosk) mode for package: $pkg", AuditSeverity.INFO)
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        logEvent(context, "LOCK_TASK_EXIT", "Device exited LockTask mode", AuditSeverity.INFO)
    }

    override fun onPasswordChanged(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordChanged(context, intent, user)
        logEvent(context, "PASSWORD_CHANGED", "Device lock screen credential changed", AuditSeverity.INFO)
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        logEvent(context, "PASSWORD_FAILED", "Failed unlock attempt detected", AuditSeverity.WARNING)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordSucceeded(context, intent, user)
    }

    private fun logEvent(context: Context, action: String, details: String, severity: AuditSeverity) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = AppDatabase.getDatabase(context)
                db.auditLogDao().insertLog(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        actorEmail = "system@enterprise.local",
                        actorRole = AdminRole.SUPER_ADMIN,
                        action = action,
                        targetDeviceId = "LOCAL_DEVICE",
                        targetDeviceName = android.os.Build.MODEL,
                        severity = severity,
                        details = details
                    )
                )
            }
        }
    }
}
