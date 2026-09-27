package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceOwnerStatus
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EnterpriseBlue
import com.example.ui.theme.EnterpriseSurfaceVariant
import com.example.ui.theme.WarningAmber

@Composable
fun RoleSelectionScreen(
    deviceOwnerStatus: DeviceOwnerStatus,
    onNavigateToAdmin: () -> Unit,
    onNavigateToAgent: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Hero Brand Section
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
                .border(2.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Enterprise Shield",
                tint = CyberCyan,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DROIDCOMMAND",
            style = MaterialTheme.typography.titleMedium.copy(
                letterSpacing = 4.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = CyberCyan
        )

        Text(
            text = "Ultimate Enterprise MDM",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Persistent Android Enterprise Device Owner management, policy enforcement & authorized live remote support.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Device Authority Status Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (deviceOwnerStatus == DeviceOwnerStatus.DEVICE_OWNER) ComplianceGreen else WarningAmber
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (deviceOwnerStatus == DeviceOwnerStatus.DEVICE_OWNER) Icons.Default.Verified else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (deviceOwnerStatus == DeviceOwnerStatus.DEVICE_OWNER) ComplianceGreen else WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (deviceOwnerStatus) {
                        DeviceOwnerStatus.DEVICE_OWNER -> "DEVICE OWNER ACTIVE (MAXIMUM AUTHORITY)"
                        DeviceOwnerStatus.PROFILE_OWNER -> "PROFILE OWNER ACTIVE (MANAGED WORK PROFILE)"
                        DeviceOwnerStatus.ENROLLMENT_PENDING -> "DEVICE ADMIN ACTIVE (ENROLLMENT PENDING)"
                        else -> "STANDARD INSTALLATION (REQUIRES PROVISIONING)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (deviceOwnerStatus == DeviceOwnerStatus.DEVICE_OWNER) ComplianceGreen else WarningAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "SELECT OPERATIONAL MODE",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mode 1: Administrator Console
        RoleCard(
            title = "Administrator Console",
            subtitle = "Administer fleet, enforce policies, review compliance & initiate live remote support sessions.",
            badge = "SUPER ADMIN / OPERATOR",
            icon = Icons.Default.AdminPanelSettings,
            accentColor = CyberCyan,
            testTag = "role_admin_card",
            onClick = onNavigateToAdmin
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode 2: Managed Device Agent
        RoleCard(
            title = "Managed Device Agent",
            subtitle = "View enterprise enrollment status, active policy restrictions, and manage remote assistance requests.",
            badge = "MANAGED ENDPOINT",
            icon = Icons.Default.PhoneAndroid,
            accentColor = EnterpriseBlue,
            testTag = "role_agent_card",
            onClick = onNavigateToAgent
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Setup Guide Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigateToGuide() }
                .testTag("setup_guide_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Owner Provisioning Guide",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Android Enterprise QR, Zero-Touch & ADB setup steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Badge(
                    containerColor = accentColor.copy(alpha = 0.2f),
                    contentColor = accentColor
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launch Mode",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
