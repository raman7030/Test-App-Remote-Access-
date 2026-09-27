package com.example.webrtc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class WebRtcConnectionState {
    IDLE, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, FAILED
}

data class WebRtcQualityConfig(
    val resolution: String = "1080p (1080x2400)",
    val targetFps: Int = 30,
    val targetBitrateKbps: Int = 2500,
    val codec: String = "H.264 / VP8",
    val encryption: String = "DTLS-SRTP"
)

data class WebRtcSessionStats(
    val connectionState: WebRtcConnectionState = WebRtcConnectionState.IDLE,
    val rttMs: Long = 0L,
    val currentFps: Int = 0,
    val currentBitrateKbps: Int = 0,
    val packetLossPercent: Float = 0f,
    val framesSent: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val isPaused: Boolean = false
)

/**
 * Local session state and an in-process event bus.
 *
 * This class is deliberately NOT presented as internet signaling: production remote
 * support must connect it to an authenticated signaling service and an actual WebRTC
 * PeerConnection. Only events received from a verified, authorized session should be
 * forwarded here by that transport.
 */
object WebRtcSignalingManager {
    private val _sessionStats = MutableStateFlow(WebRtcSessionStats())
    val sessionStats: StateFlow<WebRtcSessionStats> = _sessionStats.asStateFlow()

    private val _qualityConfig = MutableStateFlow(WebRtcQualityConfig())
    val qualityConfig: StateFlow<WebRtcQualityConfig> = _qualityConfig.asStateFlow()

    private val _incomingGestureEvents =
        MutableSharedFlow<com.example.model.RemoteGesture>(extraBufferCapacity = 64)
    val incomingGestureEvents: SharedFlow<com.example.model.RemoteGesture> =
        _incomingGestureEvents.asSharedFlow()

    @Volatile private var activeSessionId: String? = null
    @Volatile private var localConsentGranted: Boolean = false
    @Volatile private var remoteOperatorAuthorized: Boolean = false

    fun beginConsentSession(): String {
        val id = UUID.randomUUID().toString()
        activeSessionId = id
        localConsentGranted = true
        remoteOperatorAuthorized = false
        _sessionStats.value = WebRtcSessionStats(connectionState = WebRtcConnectionState.CONNECTING)
        return id
    }

    fun authorizeOperator(sessionId: String, authorized: Boolean): Boolean {
        if (sessionId != activeSessionId || !localConsentGranted) return false
        remoteOperatorAuthorized = authorized
        return true
    }

    fun isRemoteInputAuthorized(sessionId: String): Boolean =
        sessionId == activeSessionId && localConsentGranted && remoteOperatorAuthorized

    fun endConsentSession() {
        localConsentGranted = false
        remoteOperatorAuthorized = false
        activeSessionId = null
        reset()
    }

    fun updateConnectionState(state: WebRtcConnectionState) {
        _sessionStats.value = _sessionStats.value.copy(connectionState = state)
    }

    fun updateStats(rttMs: Long, fps: Int, bitrate: Int, packetLoss: Float, frames: Long, duration: Long) {
        _sessionStats.value = _sessionStats.value.copy(
            rttMs = rttMs.coerceAtLeast(0),
            currentFps = fps.coerceAtLeast(0),
            currentBitrateKbps = bitrate.coerceAtLeast(0),
            packetLossPercent = packetLoss.coerceIn(0f, 100f),
            framesSent = frames.coerceAtLeast(0),
            sessionDurationSeconds = duration.coerceAtLeast(0)
        )
    }

    fun setQualityConfig(config: WebRtcQualityConfig) {
        _qualityConfig.value = config.copy(
            targetFps = config.targetFps.coerceIn(1, 60),
            targetBitrateKbps = config.targetBitrateKbps.coerceIn(128, 20_000)
        )
    }

    fun togglePause() {
        _sessionStats.value = _sessionStats.value.copy(isPaused = !_sessionStats.value.isPaused)
    }

    /**
     * Internal bridge only. Call only after server-side authentication, session matching,
     * replay protection, and explicit local consent have been verified.
     */
    fun dispatchAuthorizedGesture(sessionId: String, gesture: com.example.model.RemoteGesture): Boolean {
        if (!isRemoteInputAuthorized(sessionId)) return false
        return _incomingGestureEvents.tryEmit(gesture)
    }

    @Deprecated("Use dispatchAuthorizedGesture with a verified session ID")
    fun sendRemoteGesture(gesture: com.example.model.RemoteGesture) {
        // Legacy method intentionally does not dispatch unauthenticated remote input.
    }

    fun reset() {
        _sessionStats.value = WebRtcSessionStats()
    }
}
