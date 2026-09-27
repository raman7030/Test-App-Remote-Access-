package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceOwnerStatus
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EnterpriseBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ManagedAgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAgentStatusScreen(
    viewModel: ManagedAgentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val report by viewModel.capabilityReport.collectAsState()
    val isSharing by viewModel.isScreenSharing.collectAsState()
    val isAccessibilityAuth by viewModel.isAccessibilityAuthorized.collectAsState()
    val incomingRequest by viewModel.incomingSupportRequest.collectAsState()
    val policies by viewModel.localPolicies.collectAsState()
    val sessionStats by viewModel.currentSessionStats.collectAsState()

    // Activity Result Launcher for MediaProjection Intent
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleProjectionResult(context, result.resultCode, result.data)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Managed Device Endpoint", fontWeight = FontWeight.Bold)
                        Text("Persistent Trust & Client Agent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("agent_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }, modifier = Modifier.testTag("agent_refresh_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Enterprise Management Disclosure Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (report.isDeviceOwner) CyberCyan.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (report.isDeviceOwner) ComplianceGreen else WarningAmber
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (report.isDeviceOwner) Icons.Default.Verified else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (report.isDeviceOwner) ComplianceGreen else WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (report.isDeviceOwner) "Device Managed by Enterprise Headquarters" else "Standard Android App Installation",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Device ID: ${viewModel.localDeviceId} • Fingerprint: ${viewModel.localFingerprint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "This device is subject to enterprise monitoring and corporate data protection policies. " +
                                    "Your administrator can configure restrictions, install authorized software, and request remote screen assistance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ACTIVE SCREEN SHARING ALERT WITH KILL SWITCH!
            if (isSharing) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_screen_share_card"),
                        colors = CardDefaults.cardColors(containerColor = CriticalRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, CriticalRed)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(CriticalRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SCREEN SHARING CURRENTLY ACTIVE",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = CriticalRed
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Your screen is being viewed by an authorized IT administrator over an encrypted DTLS-SRTP WebRTC stream. " +
                                        "Active duration: ${sessionStats.sessionDurationSeconds}s.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.stopScreenSharing(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stop_screen_sharing_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CriticalRed, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STOP SCREEN SHARING IMMEDIATELY", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Accessibility Remote Assistance Authorization Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = EnterpriseBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Remote Touch Assistance",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isAccessibilityAuth) "Authorized & Service Active" else "Requires Accessibility Authorization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAccessibilityAuth) ComplianceGreen else WarningAmber
                                    )
                                }
                            }

                            Switch(
                                checked = isAccessibilityAuth,
                                onCheckedChange = { viewModel.toggleAccessibilityAuthorization(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = EnterpriseBlue)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Allows your support technician to tap, scroll, and assist on your screen during approved remote sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Android Accessibility Settings")
                        }
                    }
                }
            }

            // Test Remote Support Session Simulation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Session Diagnostics & Testing", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Simulate an incoming support request from an administrator to test the consent prompt and screen capture flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.simulateIncomingSupportRequest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_support_req_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Incoming Support Request")
                        }
                    }
                }
            }

            // Enterprise Policies Currently Enforced
            item {
                Text(
                    text = "ACTIVE ENFORCED RESTRICTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(policies) { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(p.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Badge(
                            containerColor = if (p.isEnforced) ComplianceGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (p.isEnforced) ComplianceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = if (p.isEnforced) "ENFORCED" else "INACTIVE",
                                modifier = Modifier.padding(4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Diagnostics Checklist
            item {
                Text(
                    text = "PLATFORM AUTHORITY DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(report.diagnostics) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isSupported) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (item.isSupported) ComplianceGreen else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(item.statusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Unenroll / Revoke Trust
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.unenrollDevice() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unenroll & Revoke Local Credentials")
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // INCOMING SUPPORT SESSION PROMPT DIALOG
    if (incomingRequest) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSupportRequest() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CastConnected, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remote Support Request", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Administrator Sarah Jenkins (Super Admin) is requesting permission to start an authorized remote support session.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Live screen viewing via secure WebRTC\n" +
                                "• Remote touch navigation (if authorized)\n" +
                                "• A visible screen sharing icon will be shown\n" +
                                "• You can terminate this session at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptSupportSession(context, projectionLauncher) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("Grant & Start Sharing", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSupportRequest() }) {
                    Text("Decline")
                }
            }
        )
    }
}
