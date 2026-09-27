package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.security.KeystoreManager
import com.example.service.DeviceManagementService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val keystoreManager = KeystoreManager(context)
            if (keystoreManager.getEnrolledOrgId() != null) {
                val serviceIntent = Intent(context, DeviceManagementService::class.java).apply {
                    action = DeviceManagementService.ACTION_RECONNECT_SYNC
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
