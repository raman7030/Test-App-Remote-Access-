package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.webrtc.WebRtcConnectionState
import com.example.webrtc.WebRtcSignalingManager

/**
 * User-approved screen-capture foreground service.
 *
 * Android's MediaProjection consent dialog is mandatory. This implementation creates
 * and releases a VirtualDisplay, but does not silently transmit or persist captured
 * frames. A separately authenticated WebRTC transport must be integrated before
 * remote streaming can be considered functional.
 */
class ScreenCaptureService : Service() {
    companion object {
        const val ACTION_START_STREAM = "com.example.service.action.START_STREAM"
        const val ACTION_STOP_STREAM = "com.example.service.action.STOP_STREAM"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "screen_share_channel"

        @Volatile var isRunning: Boolean = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAM -> {
                if (isRunning) return START_NOT_STICKY
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == 0 || resultData == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundWithNotification()
                startProjection(resultCode, resultData)
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
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.screen_share_active_title))
            .setContentText("Screen capture is active. Tap Stop to end sharing.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop sharing", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, resultData: Intent) {
        runCatching {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, resultData)
                ?: throw IllegalStateException("Android did not grant screen capture")
            mediaProjection = projection

            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopStream()
                    stopSelf()
                }
            }, android.os.Handler(mainLooper))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

            virtualDisplay = projection.createVirtualDisplay(
                "DroidCommand-UserApproved-ScreenShare",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null,
                null,
                null
            ) ?: throw IllegalStateException("Unable to create capture display")

            isRunning = true
            WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.CONNECTING)
            // No fake CONNECTED status: network/WebRTC connection is not implemented here.
        }.onFailure {
            WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.FAILED)
            stopStream()
            stopSelf()
        }
    }

    private fun stopStream() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        val projection = mediaProjection
        mediaProjection = null
        isRunning = false
        runCatching { projection?.stop() }
        WebRtcSignalingManager.endConsentSession()
        WebRtcSignalingManager.updateConnectionState(WebRtcConnectionState.DISCONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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
                description = "Visible notification while the user-approved screen capture is active"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
