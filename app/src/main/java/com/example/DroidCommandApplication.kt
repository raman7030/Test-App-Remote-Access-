package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.dpm.DeviceCapabilityDetector
import com.example.dpm.EnterprisePolicyManager
import com.example.repository.EnterpriseRepository
import com.example.security.KeystoreManager
import com.example.service.DeviceManagementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DroidCommandApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var keystoreManager: KeystoreManager
        private set
    lateinit var policyManager: EnterprisePolicyManager
        private set
    lateinit var capabilityDetector: DeviceCapabilityDetector
        private set
    lateinit var repository: EnterpriseRepository
        private set

    companion object {
        lateinit var instance: DroidCommandApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        keystoreManager = KeystoreManager(this)
        policyManager = EnterprisePolicyManager(this)
        capabilityDetector = DeviceCapabilityDetector(this)

        repository = EnterpriseRepository(
            context = this,
            database = database,
            keystoreManager = keystoreManager,
            policyManager = policyManager,
            capabilityDetector = capabilityDetector
        )

        applicationScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Start background persistent management service
        runCatching {
            val mgmtIntent = Intent(this, DeviceManagementService::class.java).apply {
                action = DeviceManagementService.ACTION_START_MANAGEMENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(mgmtIntent)
            } else {
                startService(mgmtIntent)
            }
        }
    }
}
