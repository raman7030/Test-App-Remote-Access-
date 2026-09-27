package com.example.ui.screens

import android.accessibilityservice.AccessibilityService
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditSeverity
import com.example.model.CommandStatus
import com.example.model.CommandType
import com.example.model.ComplianceState
import com.example.model.DeviceOwnerStatus
import com.example.model.GestureType
import com.example.model.ManagedDevice
import com.example.model.ManagedPolicy
import com.example.model.ManagementCommand
import com.example.model.RemoteGesture
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EnterpriseBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.allDevices.collectAsState()
    val device = devices.find { it.id == deviceId }
    val policies by viewModel.getDevicePolicies(deviceId).collectAsState(initial = emptyList())
    val commands by viewModel.getDeviceCommands(deviceId).collectAsState(initial = emptyList())
    val sessionStats by viewModel.currentSessionStats.collectAsState()
    val qualityConfig by viewModel.qualityConfig.collectAsState()
    val toastMessage by viewModel.commandToast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var textInputPayload by remember { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Device not found: $deviceId", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${device.id} • ${device.telemetry.manufacturer} ${device.telemetry.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("device_detail_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.sendCommand(deviceId, CommandType.REFRESH_INVENTORY) },
                        modifier = Modifier.testTag("refresh_device_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Telemetry", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Strip
            DeviceStatusStrip(device = device)

            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyberCyan
            ) {
                val tabs = listOf("Telemetry", "Policies", "Remote Screen", "Commands", "Audit")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> DeviceTelemetryTab(device = device)
                1 -> DevicePoliciesTab(
                    policies = policies,
                    onTogglePolicy = { key, enable -> viewModel.togglePolicy(deviceId, key, enable) }
                )
                2 -> DeviceRemoteScreenTab(
                    device = device,
                    sessionStats = sessionStats,
                    qualityConfig = qualityConfig,
                    textInputPayload = textInputPayload,
                    onTextInputChange = { textInputPayload = it },
                    onSendTextInput = {
                        viewModel.sendTouchGesture(
                            RemoteGesture(type = GestureType.TEXT_INPUT, x = 0f, y = 0f, textPayload = textInputPayload)
                        )
                        textInputPayload = ""
                    },
                    onSendGesture = { viewModel.sendTouchGesture(it) },
                    onStartSupport = { viewModel.startRemoteSupport(deviceId) },
                    onEndSupport = { viewModel.endRemoteSupport(deviceId) },
                    onTogglePause = { viewModel.toggleStreamPause() },
                    onSetQuality = { viewModel.setQualityPreset(it) }
                )
                3 -> DeviceCommandsTab(
                    commands = commands,
                    onSendCommand = { viewModel.sendCommand(deviceId, it) },
                    onRequestWipe = { showWipeDialog = true }
                )
                4 -> DeviceAuditTab(deviceId = deviceId, viewModel = viewModel)
            }
        }
    }

    // Safety Wipe Confirmation Dialog
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CriticalRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AUTHORIZATION REQUIRED", color = CriticalRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "You are about to issue an ENTERPRISE WIPE command to ${device.name}. " +
                            "This will permanently erase all managed corporate containers, credentials, and factory reset the device. " +
                            "This action cannot be undone and will be logged in security audit records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        viewModel.sendCommand(deviceId, CommandType.ENTERPRISE_WIPE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    Text("AUTHORIZE & EXECUTE WIPE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceStatusStrip(device: ManagedDevice) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (device.isOnline) ComplianceGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (device.isOnline) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (device.isOnline) ComplianceGreen else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Group: ${device.deviceGroup}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Badge(
                containerColor = when (device.complianceState) {
                    ComplianceState.COMPLIANT -> ComplianceGreen.copy(alpha = 0.2f)
                    ComplianceState.WARNING -> WarningAmber.copy(alpha = 0.2f)
                    else -> CriticalRed.copy(alpha = 0.2f)
                },
                contentColor = when (device.complianceState) {
                    ComplianceState.COMPLIANT -> ComplianceGreen
                    ComplianceState.WARNING -> WarningAmber
                    else -> CriticalRed
                }
            ) {
                Text(
                    text = device.complianceState.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun DeviceTelemetryTab(device: ManagedDevice) {
    val t = device.telemetry
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "HARDWARE & SECURITY POSTURE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryRow("Operating System", "${t.osVersion} (API ${t.apiLevel})")
                    TelemetryRow("Security Patch", t.securityPatch)
                    TelemetryRow("Manufacturer", t.manufacturer)
                    TelemetryRow("Model Number", t.model)
                    TelemetryRow("Serial / Hardware ID", t.serialNumber)
                    TelemetryRow("Storage Encryption", if (t.isEncrypted) "Hardware-Backed AES-256" else "Disabled")
                    TelemetryRow("Biometrics Enrolled", if (t.isBiometricEnrolled) "Class 3 Strong Biometrics" else "None")
                    TelemetryRow("Keystore Security", "AndroidKeyStore Hardware Attestation")
                }
            }
        }

        item {
            Text(
                text = "RESOURCE CONSUMPTION & NETWORK",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Battery
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Battery Level", style = MaterialTheme.typography.bodyMedium)
                            Text("${t.batteryLevel}%", fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { t.batteryLevel / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (t.batteryLevel > 20) ComplianceGreen else CriticalRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Storage
                    Column {
                        val usedGb = t.storageUsedBytes / 1_000_000_000
                        val totalGb = if (t.storageTotalBytes > 0) t.storageTotalBytes / 1_000_000_000 else 128
                        val percent = usedGb.toFloat() / totalGb.toFloat()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Internal Storage", style = MaterialTheme.typography.bodyMedium)
                            Text("$usedGb GB / $totalGb GB", fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EnterpriseBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    TelemetryRow("Network Connection", t.networkType)
                    TelemetryRow("Assigned IP Address", t.ipAddress)
                    TelemetryRow("Signal Quality", "${t.signalStrength} / 4 Bars")
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DevicePoliciesTab(
    policies: List<ManagedPolicy>,
    onTogglePolicy: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ENTERPRISE POLICY ENFORCEMENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(policies, key = { it.key }) { policy ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = policy.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = policy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (policy.isEnforced) ComplianceGreen else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (policy.isEnforced) "Enforced on OS" else (policy.failureReason ?: "Pending Sync"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (policy.isEnforced) ComplianceGreen else WarningAmber
                            )
                        }
                    }

                    Switch(
                        checked = policy.isEnabled,
                        onCheckedChange = { onTogglePolicy(policy.key, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRemoteScreenTab(
    device: ManagedDevice,
    sessionStats: com.example.webrtc.WebRtcSessionStats,
    qualityConfig: com.example.webrtc.WebRtcQualityConfig,
    textInputPayload: String,
    onTextInputChange: (String) -> Unit,
    onSendTextInput: () -> Unit,
    onSendGesture: (RemoteGesture) -> Unit,
    onStartSupport: () -> Unit,
    onEndSupport: () -> Unit,
    onTogglePause: () -> Unit,
    onSetQuality: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stream Viewport / Screen Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = androidx.compose.foundation.BorderStroke(2.dp, if (device.isScreenSharingActive) CyberCyan else Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(device.isScreenSharingActive) {
                            if (device.isScreenSharingActive) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        onSendGesture(RemoteGesture(type = GestureType.TAP, x = offset.x, y = offset.y))
                                    },
                                    onLongPress = { offset ->
                                        onSendGesture(RemoteGesture(type = GestureType.LONG_PRESS, x = offset.x, y = offset.y))
                                    }
                                )
                            }
                        }
                ) {
                    if (device.isScreenSharingActive) {
                        // Simulated Live Screen Display
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Dark background
                            drawRect(Color(0xFF0F172A))
                            // Header bar simulation
                            drawRect(Color(0xFF1E293B), size = androidx.compose.ui.geometry.Size(size.width, 50f))
                            // App card 1
                            drawRoundRect(
                                color = Color(0xFF334155),
                                topLeft = Offset(40f, 90f),
                                size = androidx.compose.ui.geometry.Size(size.width - 80f, 140f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                            )
                            // App card 2
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(40f, 260f),
                                size = androidx.compose.ui.geometry.Size(size.width - 80f, 200f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                            )
                        }

                        // Live Stream Indicator & Metrics HUD
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(CriticalRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE REMOTE SESSION",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = CriticalRed
                                    )
                                }
                                Text(
                                    text = "${sessionStats.sessionDurationSeconds}s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Stats HUD
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("RTT: ${sessionStats.rttMs}ms", fontSize = 10.sp, color = CyberCyan)
                                    Text("FPS: ${sessionStats.currentFps}", fontSize = 10.sp, color = ComplianceGreen)
                                    Text("${sessionStats.currentBitrateKbps} kbps", fontSize = 10.sp, color = Color.White)
                                    Text("Loss: ${(sessionStats.packetLossPercent * 100).toInt()}%", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }

                        // Bottom Navigation Simulated Controls
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = {
                                onSendGesture(RemoteGesture(type = GestureType.GLOBAL_ACTION, x = 0f, y = 0f, actionCode = AccessibilityService.GLOBAL_ACTION_BACK))
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberCyan)
                            }
                            IconButton(onClick = {
                                onSendGesture(RemoteGesture(type = GestureType.GLOBAL_ACTION, x = 0f, y = 0f, actionCode = AccessibilityService.GLOBAL_ACTION_HOME))
                            }) {
                                Icon(Icons.Default.Home, contentDescription = "Home", tint = CyberCyan)
                            }
                            IconButton(onClick = {
                                onSendGesture(RemoteGesture(type = GestureType.GLOBAL_ACTION, x = 0f, y = 0f, actionCode = AccessibilityService.GLOBAL_ACTION_RECENTS))
                            }) {
                                Icon(Icons.Default.ViewCarousel, contentDescription = "Recents", tint = CyberCyan)
                            }
                            IconButton(onClick = {
                                onSendGesture(RemoteGesture(type = GestureType.GLOBAL_ACTION, x = 0f, y = 0f, actionCode = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = CyberCyan)
                            }
                        }
                    } else {
                        // Offline / Inactive Placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Remote Screen Sharing Inactive", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Request authorized MediaProjection screen stream over secure WebRTC DTLS-SRTP transport.",
                                color = Color.DarkGray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Live Controls Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.isScreenSharingActive) {
                    Button(
                        onClick = onEndSupport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                    ) {
                        Text("Terminate Stream", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTogglePause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (sessionStats.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (sessionStats.isPaused) "Resume" else "Pause")
                    }
                } else {
                    Button(
                        onClick = onStartSupport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text("Initiate Remote Screen Support", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Text Injection Card
        if (device.isScreenSharingActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Remote Text & Keystroke Injection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = textInputPayload,
                                onValueChange = onTextInputChange,
                                placeholder = { Text("Type text to inject into active field...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onSendTextInput,
                                enabled = textInputPayload.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                            ) {
                                Text("Inject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCommandsTab(
    commands: List<ManagementCommand>,
    onSendCommand: (CommandType) -> Unit,
    onRequestWipe: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "DIRECT MANAGEMENT ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSendCommand(CommandType.LOCK_DEVICE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Device")
                        }
                        Button(
                            onClick = { onSendCommand(CommandType.REBOOT_DEVICE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reboot (DO)")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSendCommand(CommandType.DISALLOW_CAMERA) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Block Camera")
                        }
                        Button(
                            onClick = { onSendCommand(CommandType.ALLOW_CAMERA) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Allow Camera")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onRequestWipe,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalRed.copy(alpha = 0.15f), contentColor = CriticalRed)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enterprise Wipe / Factory Reset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "COMMAND DISPATCH HISTORY & SIGNATURES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (commands.isEmpty()) {
            item {
                Text("No commands issued to this device yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            items(commands, key = { it.id }) { cmd ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cmd.type.name, fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 14.sp)
                            Badge(
                                containerColor = if (cmd.status == CommandStatus.EXECUTED) ComplianceGreen.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                contentColor = if (cmd.status == CommandStatus.EXECUTED) ComplianceGreen else WarningAmber
                            ) {
                                Text(cmd.status.name, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ID: ${cmd.id} • Admin: ${cmd.adminName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Signature: ${cmd.signature.take(24)}...", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAuditTab(deviceId: String, viewModel: AdminViewModel) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val deviceLogs = auditLogs.filter { it.targetDeviceId == deviceId || it.targetDeviceId == null }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "AUDIT TRAIL & COMPLIANCE RECORDS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (deviceLogs.isEmpty()) {
            item {
                Text("No audit events recorded for this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            items(deviceLogs, key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text(
                                text = log.severity.name,
                                color = when (log.severity) {
                                    AuditSeverity.CRITICAL -> CriticalRed
                                    AuditSeverity.WARNING -> WarningAmber
                                    else -> CyberCyan
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${log.actorEmail} • ${dateFormat.format(Date(log.timestamp))}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
