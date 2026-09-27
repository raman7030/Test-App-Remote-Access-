package com.example.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.auth.FirebaseSupportSessions
import kotlinx.coroutines.launch

private const val PAIRING_CHANNEL_ID = "pairing_code_channel"
private const val PAIRING_NOTIFICATION_ID = 2101

private fun showPairingCodeNotification(context: Context, code: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                PAIRING_CHANNEL_ID,
                "Remote support pairing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays the active, user-created remote support pairing code."
            }
        )
    }
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val openApp = PendingIntent.getActivity(
        context, 2101, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, PAIRING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("DroidCommand pairing code")
        .setContentText("Active session code: $code")
        .setStyle(NotificationCompat.BigTextStyle().bigText("Your support pairing code is $code. Share it only with the intended operator. The session expires automatically; approve assistance on the device before capture can begin."))
        .setContentIntent(openApp)
        .setAutoCancel(false)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    manager.notify(PAIRING_NOTIFICATION_ID, notification)
}

@Composable
fun SupportSessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sessions = remember { FirebaseSupportSessions() }
    var joinCode by remember { mutableStateOf("") }
    var activeCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) status = "Notifications are disabled. You can still view the code in the app, but Android will not show its notification."
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun runTask(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try { block() } catch (e: Exception) { status = e.message ?: "Request failed." }
            finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Consent-based support session", style = MaterialTheme.typography.headlineSmall)
        Text("Create a short-lived pairing code on this device, or join a code supplied by its owner. The device owner must separately approve assistance.")
        Button(enabled = !busy, onClick = {
            runTask {
                activeCode = sessions.createSession()
                showPairingCodeNotification(context, activeCode)
                status = "Pairing code created and posted to notifications when notification permission is granted. Share it only with the intended operator."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Create device session (15 min)") }
        if (activeCode.isNotBlank()) {
            Text("Session code: $activeCode", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                activeCode = ""
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(PAIRING_NOTIFICATION_ID)
                status = "Code hidden locally and notification dismissed. The session remains pending until it expires or is ended."
            }, modifier = Modifier.fillMaxWidth()) { Text("Hide code and dismiss notification") }
            Button(enabled = !busy, onClick = { runTask { sessions.setConsent(activeCode, true); status = "Consent recorded. Screen capture still requires Android's separate system prompt." } }, modifier = Modifier.fillMaxWidth()) { Text("Approve waiting operator") }
            OutlinedButton(enabled = !busy, onClick = { runTask { sessions.setConsent(activeCode, false); (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(PAIRING_NOTIFICATION_ID); status = "Request denied." } }, modifier = Modifier.fillMaxWidth()) { Text("Deny request") }
            OutlinedButton(enabled = !busy, onClick = { runTask { sessions.endSession(activeCode); activeCode = ""; (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(PAIRING_NOTIFICATION_ID); status = "Session ended." } }, modifier = Modifier.fillMaxWidth()) { Text("End session") }
        }
        Divider()
        OutlinedTextField(value = joinCode, onValueChange = { joinCode = it.trim().take(64) }, label = { Text("Owner's pairing code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && joinCode.isNotBlank(), onClick = {
            runTask { val s = sessions.joinSession(joinCode); activeCode = s.id; status = "Join request sent. Waiting for device-owner consent." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Request to join as operator") }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.weight(1f))
        Text("Android does not allow an app to silently grant every permission. Notifications can be requested here; accessibility must be enabled by the user in Settings, and screen capture requires Android's system consent each session. This screen manages session metadata only; live streaming and remote input transport are not yet implemented.")
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
