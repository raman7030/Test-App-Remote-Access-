package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.DroidCommandApplication
import com.example.dpm.DeviceCapabilityReport
import com.example.model.ManagedPolicy
import com.example.service.RemoteAccessibilityService
import com.example.service.ScreenCaptureService
import com.example.webrtc.WebRtcSessionStats
import com.example.webrtc.WebRtcSignalingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManagedAgentViewModel : ViewModel() {

    private val app = DroidCommandApplication.instance
    private val capabilityDetector = app.capabilityDetector
    private val policyManager = app.policyManager
    private val keystoreManager = app.keystoreManager
    private val repository = app.repository

    private val _capabilityReport = MutableStateFlow(capabilityDetector.getReport())
    val capabilityReport: StateFlow<DeviceCapabilityReport> = _capabilityReport.asStateFlow()

    private val _incomingSupportRequest = MutableStateFlow<Boolean>(false)
    val incomingSupportRequest: StateFlow<Boolean> = _incomingSupportRequest.asStateFlow()

    private val _isScreenSharing = MutableStateFlow(ScreenCaptureService.isRunning)
    val isScreenSharing: StateFlow<Boolean> = _isScreenSharing.asStateFlow()

    private val _isAccessibilityAuthorized = MutableStateFlow(RemoteAccessibilityService.isServiceActive)
    val isAccessibilityAuthorized: StateFlow<Boolean> = _isAccessibilityAuthorized.asStateFlow()

    val currentSessionStats: StateFlow<WebRtcSessionStats> = WebRtcSignalingManager.sessionStats

    val localDeviceId = keystoreManager.getDeviceUuid()
    val localFingerprint = keystoreManager.getDeviceIdentityFingerprint()

    val localPolicies: StateFlow<List<ManagedPolicy>> = repository.getPoliciesForDevice(localDeviceId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        policyManager.getStandardPoliciesList()
    )

    fun refreshStatus() {
        _capabilityReport.value = capabilityDetector.getReport()
        _isScreenSharing.value = ScreenCaptureService.isRunning
        _isAccessibilityAuthorized.value = RemoteAccessibilityService.isServiceActive
    }

    fun simulateIncomingSupportRequest() {
        _incomingSupportRequest.value = true
    }

    fun dismissSupportRequest() {
        _incomingSupportRequest.value = false
    }

    fun acceptSupportSession(context: Context, projectionLauncher: ActivityResultLauncher<Intent>) {
        _incomingSupportRequest.value = false
        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpManager.createScreenCaptureIntent())
    }

    fun handleProjectionResult(context: Context, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_STREAM
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isScreenSharing.value = true
            viewModelScope.launch {
                repository.updateScreenSharingStatus(localDeviceId, true, "ACTIVE-STREAM")
            }
        }
    }

    fun stopScreenSharing(context: Context) {
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP_STREAM
        }
        context.startService(intent)
        _isScreenSharing.value = false
        viewModelScope.launch {
            repository.updateScreenSharingStatus(localDeviceId, false, null)
        }
    }

    fun toggleAccessibilityAuthorization(authorized: Boolean) {
        // Consent can only be granted through the explicit per-session approval flow.
        if (!authorized) RemoteAccessibilityService.revokeSessionConsent()
        _isAccessibilityAuthorized.value = RemoteAccessibilityService.isServiceActive
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun unenrollDevice() {
        keystoreManager.clearCredentials()
        refreshStatus()
    }
}
