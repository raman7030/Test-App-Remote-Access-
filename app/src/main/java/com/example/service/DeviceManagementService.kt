package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.model.CommandStatus
import com.example.security.KeystoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeviceManagementService : Service() {

    companion object {
        const val ACTION_START_MANAGEMENT = "com.example.service.action.START_MANAGEMENT"
        const val ACTION_RECONNECT_SYNC = "com.example.service.action.RECONNECT_SYNC"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "device_mgmt_channel"
        var isServiceRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        setupNetworkCallback()
        startHeartbeatSync()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RECONNECT_SYNC) {
            serviceScope.launch {
                triggerSync()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.mgmt_service_active_title))
            .setContentText(getString(R.string.mgmt_service_active_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                serviceScope.launch {
                    triggerSync()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                serviceScope.launch {
                    updateDeviceOnlineStatus(false)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun startHeartbeatSync() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(45000) // 45s heartbeat
                triggerSync()
            }
        }
    }

    private suspend fun triggerSync() {
        val keystoreManager = KeystoreManager(this)
        if (keystoreManager.isTokenExpired()) {
            keystoreManager.rotateTokens()
        }
        val deviceId = keystoreManager.getDeviceUuid()
        updateDeviceOnlineStatus(true)
        processPendingCommands(deviceId)
    }

    private suspend fun updateDeviceOnlineStatus(isOnline: Boolean) {
        runCatching {
            val db = AppDatabase.getDatabase(this)
            val deviceId = KeystoreManager(this).getDeviceUuid()
            db.deviceDao().updateOnlineStatus(deviceId, isOnline, System.currentTimeMillis())
        }
    }

    private suspend fun processPendingCommands(deviceId: String) {
        runCatching {
            val db = AppDatabase.getDatabase(this)
            val pending = db.commandDao().getPendingCommands(deviceId)
            for (cmd in pending) {
                // Execute command
                db.commandDao().updateCommandStatus(
                    cmd.id,
                    CommandStatus.EXECUTED,
                    System.currentTimeMillis(),
                    "Executed via persistent management channel"
                )
            }
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        heartbeatJob?.cancel()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_mgmt),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of background enterprise management connectivity"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
