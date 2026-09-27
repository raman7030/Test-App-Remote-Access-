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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdminRole
import com.example.ui.theme.ComplianceGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EnterpriseBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRbacScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roles & Access Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("rbac_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberCyan)
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
            item {
                Text(
                    text = "ACTIVE SESSION ROLE ASSIGNMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Switch between organization roles to test permission boundaries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val roles = listOf(
                AdminRole.SUPER_ADMIN to ("Super Administrator" to "Full administrative authority: wipe, policy deployment, account delegation, and audit extraction."),
                AdminRole.DEVICE_ADMIN to ("Device Administrator" to "Fleet operations: policy toggling, lock devices, inventory refresh, and live remote support."),
                AdminRole.REMOTE_SUPPORT_OPERATOR to ("Remote Support Operator" to "Helpdesk assistance: live screen monitoring, permitted touch assistance, and session termination."),
                AdminRole.READ_ONLY_AUDITOR to ("Read-Only Auditor" to "Compliance review: view fleet telemetry, check policy enforcement, and inspect security audit logs.")
            )

            items(roles) { (role, info) ->
                val isSelected = currentRole == role
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.selectRole(role) }
                        .testTag("select_role_${role.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyberCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) CyberCyan else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) CyberCyan else Color.Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(CyberCyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.first,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = info.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "REGISTERED ENTERPRISE ADMINISTRATORS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val admins = listOf(
                Triple("Sarah Jenkins", "sarah.jenkins@enterprise.com", "Super Admin • MFA Active"),
                Triple("David Vance", "david.vance@enterprise.com", "Device Admin • MFA Active"),
                Triple("Elena Rostova", "elena.rostova@enterprise.com", "Support Operator • MFA Active"),
                Triple("Marcus Brody", "marcus.brody@enterprise.com", "Auditor • MFA Active")
            )

            items(admins) { admin ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EnterpriseBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = EnterpriseBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(admin.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(admin.second, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(admin.third, fontSize = 10.sp, color = ComplianceGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
