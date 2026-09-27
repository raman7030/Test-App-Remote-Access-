package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.webrtc.WebRtcConnectionState
import com.example.webrtc.WebRtcSignalingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START_STREAM = "com.example.service.action.START_STREAM"
        const val ACTION_STOP_STREAM = "com.example.service.action.STOP_STREAM"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "screen_share_channel"
        var isRunning: Boolean = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var statsJob: Job? = null
    private var sessionDuration = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAM -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    startForegroundWithNotification()
                    startProjection(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP_STREAM -> {
                stopStream()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP_STREAM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.screen_share_active_title))
            .setContentText(getString(R.string.screen_share_active_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Session", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning = true
    }

    private fun startProjection(resultCode: Int, resultData: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.CONNECTING)

        statsJob?.cancel()
        sessionDuration = 0L
        statsJob = serviceScope.launch {
            delay(800) // connection setup simulation
            WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.CONNECTED)

            var frameCount = 0L
            while (isActive) {
                delay(1000)
                sessionDuration++
                frameCount += 30
                WebRtcSignalingManager.updateStats(
                    rttMs = (24..35).random().toLong(),
                    fps = (29..30).random(),
                    bitrate = (2350..2520).random(),
                    packetLoss = 0.02f,
                    frames = frameCount,
                    duration = sessionDuration
                )
            }
        }
    }

    private fun stopStream() {
        statsJob?.cancel()
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        isRunning = false
        WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.DISCONNECTED)
        WebRtcSignalingManager.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopStream()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_screen_share),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows visible notification while screen is being shared with enterprise admin"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
