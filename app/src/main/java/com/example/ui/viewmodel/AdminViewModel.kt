package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.DroidCommandApplication
import com.example.dpm.EnrollmentPayloadConfig
import com.example.dpm.EnterpriseProvisioningHelper
import com.example.model.AdminRole
import com.example.model.AuditLog
import com.example.model.CommandType
import com.example.model.ComplianceState
import com.example.model.ManagedDevice
import com.example.model.ManagedPolicy
import com.example.model.ManagementCommand
import com.example.model.RemoteGesture
import com.example.webrtc.WebRtcQualityConfig
import com.example.webrtc.WebRtcSessionStats
import com.example.webrtc.WebRtcSignalingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FleetOverviewStats(
    val totalDevices: Int = 0,
    val onlineDevices: Int = 0,
    val offlineDevices: Int = 0,
    val nonCompliantDevices: Int = 0,
    val activeSessionsCount: Int = 0
)

class AdminViewModel : ViewModel() {

    private val repository = DroidCommandApplication.instance.repository

    private val _currentRole = MutableStateFlow(AdminRole.SUPER_ADMIN)
    val currentRole: StateFlow<AdminRole> = _currentRole.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // "ALL", "ONLINE", "ATTENTION", "OFFLINE"
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    val allDevices = repository.allDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredDevices: StateFlow<List<ManagedDevice>> = combine(
        allDevices,
        _searchQuery,
        _statusFilter
    ) { devices, query, filter ->
        devices.filter { device ->
            val matchesQuery = query.isBlank() ||
                    device.name.contains(query, ignoreCase = true) ||
                    device.id.contains(query, ignoreCase = true) ||
                    device.telemetry.model.contains(query, ignoreCase = true) ||
                    device.deviceGroup.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "ONLINE" -> device.isOnline
                "OFFLINE" -> !device.isOnline
                "ATTENTION" -> device.complianceState != ComplianceState.COMPLIANT
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fleetStats: StateFlow<FleetOverviewStats> = allDevices.combine(allDevices) { devices, _ ->
        FleetOverviewStats(
            totalDevices = devices.size,
            onlineDevices = devices.count { it.isOnline },
            offlineDevices = devices.count { !it.isOnline },
            nonCompliantDevices = devices.count { it.complianceState != ComplianceState.COMPLIANT },
            activeSessionsCount = devices.count { it.isScreenSharingActive }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FleetOverviewStats())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val currentSessionStats: StateFlow<WebRtcSessionStats> = WebRtcSignalingManager.sessionStats
    val qualityConfig: StateFlow<WebRtcQualityConfig> = WebRtcSignalingManager.qualityConfig

    private val _provisioningConfig = MutableStateFlow(
        EnrollmentPayloadConfig(
            organizationId = "",
            organizationName = "",
            serverUrl = "",
            enrollmentToken = "",
            signingCertificateChecksum = ""
        )
    )
    val provisioningConfig: StateFlow<EnrollmentPayloadConfig> = _provisioningConfig.asStateFlow()

    private val _qrMatrix = MutableStateFlow(emptyArray<BooleanArray>())
    val qrMatrix: StateFlow<Array<BooleanArray>> = _qrMatrix.asStateFlow()

    private val _commandToast = MutableStateFlow<String?>(null)
    val commandToast: StateFlow<String?> = _commandToast.asStateFlow()


    fun selectRole(role: AdminRole) {
        _currentRole.value = role
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun selectDevice(deviceId: String?) {
        _selectedDeviceId.value = deviceId
    }

    fun getDevicePolicies(deviceId: String): StateFlow<List<ManagedPolicy>> {
        return repository.getPoliciesForDevice(deviceId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun getDeviceCommands(deviceId: String): StateFlow<List<ManagementCommand>> {
        return repository.getCommandsForDevice(deviceId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun sendCommand(deviceId: String, type: CommandType, paramsJson: String = "{}") {
        viewModelScope.launch {
            val result = repository.sendManagementCommand(
                deviceId = deviceId,
                type = type,
                parametersJson = paramsJson,
                adminRole = _currentRole.value
            )
            result.onSuccess {
                _commandToast.value = "Command $type dispatched successfully (ID: $it)"
            }.onFailure {
                _commandToast.value = "Failed: ${it.localizedMessage}"
            }
        }
    }

    fun togglePolicy(deviceId: String, policyKey: String, enable: Boolean) {
        viewModelScope.launch {
            repository.toggleDevicePolicy(deviceId, policyKey, enable).onSuccess {
                _commandToast.value = "Policy $policyKey updated to $enable"
            }.onFailure {
                _commandToast.value = "Policy error: ${it.localizedMessage}"
            }
        }
    }

    fun startRemoteSupport(deviceId: String) {
        viewModelScope.launch {
            repository.updateScreenSharingStatus(deviceId, true, "SESSION-" + System.currentTimeMillis().toString().takeLast(6))
            sendCommand(deviceId, CommandType.REQUEST_SUPPORT_SESSION)
        }
    }

    fun endRemoteSupport(deviceId: String) {
        viewModelScope.launch {
            repository.updateScreenSharingStatus(deviceId, false, null)
            sendCommand(deviceId, CommandType.END_SUPPORT_SESSION)
            WebRtcSignalingManager.reset()
        }
    }

    fun sendTouchGesture(gesture: RemoteGesture) {
        WebRtcSignalingManager.sendRemoteGesture(gesture)
    }

    fun toggleStreamPause() {
        WebRtcSignalingManager.togglePause()
    }

    fun setQualityPreset(preset: String) {
        val config = when (preset) {
            "720p" -> WebRtcQualityConfig(resolution = "720p (720x1600)", targetFps = 30, targetBitrateKbps = 1500)
            "480p" -> WebRtcQualityConfig(resolution = "480p (480x1060)", targetFps = 15, targetBitrateKbps = 800)
            else -> WebRtcQualityConfig(resolution = "1080p (1080x2400)", targetFps = 30, targetBitrateKbps = 2500)
        }
        WebRtcSignalingManager.setQualityConfig(config)
    }

    fun updateProvisioningConfig(config: EnrollmentPayloadConfig) {
        _provisioningConfig.value = config
        refreshQrMatrix()
    }

    private fun refreshQrMatrix() {
        // Do not crash the admin UI while the standards-compliant QR encoder is unavailable.
        val config = _provisioningConfig.value
        val isComplete = config.organizationId.isNotBlank() &&
            config.organizationName.isNotBlank() &&
            config.serverUrl.startsWith("https://", ignoreCase = true) &&
            config.enrollmentToken.isNotBlank() &&
            config.signingCertificateChecksum.matches(Regex("(?i)^[a-f0-9]{64}$"))
        if (!isComplete) {
            _qrMatrix.value = emptyArray()
            return
        }
        val json = EnterpriseProvisioningHelper.generateEnterpriseProvisioningJson(
            DroidCommandApplication.instance,
            config
        )
        _qrMatrix.value = runCatching {
            EnterpriseProvisioningHelper.generateQrMatrix(json, size = 29)
        }.getOrElse {
            _commandToast.value = "Provisioning QR unavailable: QR encoder is not integrated."
            emptyArray()
        }
    }

    fun clearToast() {
        _commandToast.value = null
    }
}
