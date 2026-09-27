package com.example.webrtc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WebRtcConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}

data class WebRtcQualityConfig(
    val resolution: String = "1080p (1080x2400)",
    val targetFps: Int = 30,
    val targetBitrateKbps: Int = 2500,
    val codec: String = "H.264 / VP8",
    val encryption: String = "DTLS-SRTP (AES-GCM-256)"
)

data class WebRtcSessionStats(
    val connectionState: WebRtcConnectionState = WebRtcConnectionState.IDLE,
    val rttMs: Long = 28L,
    val currentFps: Int = 30,
    val currentBitrateKbps: Int = 2420,
    val packetLossPercent: Float = 0.05f,
    val framesSent: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val isPaused: Boolean = false
)

object WebRtcSignalingManager {

    private val _sessionStats = MutableStateFlow(WebRtcSessionStats())
    val sessionStats: StateFlow<WebRtcSessionStats> = _sessionStats.asStateFlow()

    private val _qualityConfig = MutableStateFlow(WebRtcQualityConfig())
    val qualityConfig: StateFlow<WebRtcQualityConfig> = _qualityConfig.asStateFlow()

    private val _incomingGestureEvents = MutableSharedFlow<com.example.model.RemoteGesture>(extraBufferCapacity = 64)
    val incomingGestureEvents: SharedFlow<com.example.model.RemoteGesture> = _incomingGestureEvents.asSharedFlow()

    fun updateConnectionState(state: WebRtcConnectionState) {
        _sessionStats.value = _sessionStats.value.copy(connectionState = state)
    }

    fun updateStats(rttMs: Long, fps: Int, bitrate: Int, packetLoss: Float, frames: Long, duration: Long) {
        _sessionStats.value = _sessionStats.value.copy(
            rttMs = rttMs,
            currentFps = fps,
            currentBitrateKbps = bitrate,
            packetLossPercent = packetLoss,
            framesSent = frames,
            sessionDurationSeconds = duration
        )
    }

    fun setQualityConfig(config: WebRtcQualityConfig) {
        _qualityConfig.value = config
    }

    fun togglePause() {
        val current = _sessionStats.value.isPaused
        _sessionStats.value = _sessionStats.value.copy(isPaused = !current)
    }

    fun sendRemoteGesture(gesture: com.example.model.RemoteGesture) {
        _incomingGestureEvents.tryEmit(gesture)
    }

    fun reset() {
        _sessionStats.value = WebRtcSessionStats()
    }
}
