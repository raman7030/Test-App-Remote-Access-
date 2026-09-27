package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.GoogleFirebaseAuth
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.SupportSessionScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminRbacScreen
import com.example.ui.screens.DeviceAgentStatusScreen
import com.example.ui.screens.DeviceDetailScreen
import com.example.ui.screens.DeviceOwnerGuideScreen
import com.example.ui.screens.ProvisioningQrScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.theme.DroidCommandTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ManagedAgentViewModel

class MainActivity : ComponentActivity() {

    private val adminViewModel: AdminViewModel by viewModels()
    private val agentViewModel: ManagedAgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DroidCommandTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val capabilityReport by agentViewModel.capabilityReport.collectAsState()

                    val googleAuth = androidx.compose.runtime.remember { GoogleFirebaseAuth() }
                    NavHost(
                        navController = navController,
                        startDestination = "account"
                    ) {
                        composable("account") {
                            AccountScreen(
                                activity = this@MainActivity,
                                auth = googleAuth,
                                onContinue = { navController.navigate("role_selection") { popUpTo("account") { inclusive = true } } }
                            )
                        }
                        composable("role_selection") {
                            RoleSelectionScreen(
                                deviceOwnerStatus = capabilityReport.ownershipStatus,
                                onNavigateToAdmin = { navController.navigate("admin_dashboard") },
                                onNavigateToAgent = { navController.navigate("agent_status") },
                                onNavigateToGuide = { navController.navigate("device_owner_guide") },
                                onNavigateToSupport = { navController.navigate("support_sessions") }
                            )
                        }

                        composable("support_sessions") {\n                            SupportSessionScreen(onBack = { navController.popBackStack() })\n                        }\n\n                        composable("admin_dashboard") {
                            AdminDashboardScreen(
                                viewModel = adminViewModel,
                                onSelectDevice = { deviceId ->
                                    navController.navigate("device_detail/$deviceId")
                                },
                                onOpenProvisioningQr = { navController.navigate("provisioning_qr") },
                                onOpenRbac = { navController.navigate("admin_rbac") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "device_detail/{deviceId}",
                            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                            DeviceDetailScreen(
                                deviceId = deviceId,
                                viewModel = adminViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("provisioning_qr") {
                            ProvisioningQrScreen(
                                viewModel = adminViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("admin_rbac") {
                            AdminRbacScreen(
                                viewModel = adminViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("agent_status") {
                            DeviceAgentStatusScreen(
                                viewModel = agentViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("device_owner_guide") {
                            DeviceOwnerGuideScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
