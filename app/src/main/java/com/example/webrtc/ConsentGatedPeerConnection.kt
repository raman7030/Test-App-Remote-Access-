package com.example.webrtc

import android.content.Context
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SdpObserver
import org.webrtc.VideoTrack

/**
 * Consent-gated WebRTC peer connection foundation.
 *
 * This class handles WebRTC peer setup and SDP/ICE negotiation. The app must
 * supply an authenticated signaling transport and attach a user-approved video
 * track from MediaProjection before it can mirror a screen over the network.
 * Never pass unverified signaling messages or create a peer before owner consent.
 */
class ConsentGatedPeerConnection(
    context: Context,
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onStateChanged: (WebRtcConnectionState) -> Unit
) {
    private val factory: PeerConnectionFactory
    private var peer: PeerConnection? = null
    private var closed = false

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    @Synchronized
    fun start(ownerConsentGranted: Boolean, operatorAuthenticated: Boolean): Boolean {
        check(!closed) { "Peer connection has been closed." }
        require(ownerConsentGranted) { "Device owner consent is required." }
        require(operatorAuthenticated) { "Operator authentication is required." }
        if (peer != null) return true

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peer = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                onStateChanged(
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> WebRtcConnectionState.CONNECTED
                        PeerConnection.IceConnectionState.CHECKING -> WebRtcConnectionState.CONNECTING
                        PeerConnection.IceConnectionState.DISCONNECTED -> WebRtcConnectionState.RECONNECTING
                        PeerConnection.IceConnectionState.FAILED -> WebRtcConnectionState.FAILED
                        PeerConnection.IceConnectionState.CLOSED -> WebRtcConnectionState.DISCONNECTED
                        else -> WebRtcConnectionState.CONNECTING
                    }
                )
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) = onIceCandidate.invoke(candidate)
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver, streams: Array<out org.webrtc.MediaStream>) = Unit
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) = Unit
        }) ?: run {
            onStateChanged(WebRtcConnectionState.FAILED)
            return false
        }
        onStateChanged(WebRtcConnectionState.CONNECTING)
        return true
    }

    @Synchronized
    fun addRemoteIceCandidate(candidate: IceCandidate): Boolean =
        peer?.addIceCandidate(candidate) ?: false

    fun createOffer(onCreated: (SessionDescription?) -> Unit) {
        val connection = checkNotNull(peer) { "Start an approved session first." }
        connection.createOffer(descriptionObserver(onCreated), MediaConstraints())
    }

    fun createAnswer(onCreated: (SessionDescription?) -> Unit) {
        val connection = checkNotNull(peer) { "Start an approved session first." }
        connection.createAnswer(descriptionObserver(onCreated), MediaConstraints())
    }

    fun setLocalDescription(description: SessionDescription, onComplete: (Boolean) -> Unit) {
        val connection = checkNotNull(peer) { "Start an approved session first." }
        connection.setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onSetSuccess() = onComplete(true)
            override fun onCreateFailure(error: String?) = onComplete(false)
            override fun onSetFailure(error: String?) = onComplete(false)
        }, description)
    }

    fun setRemoteDescription(description: SessionDescription, onComplete: (Boolean) -> Unit) {
        val connection = checkNotNull(peer) { "Start an approved session first." }
        connection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onSetSuccess() = onComplete(true)
            override fun onCreateFailure(error: String?) = onComplete(false)
            override fun onSetFailure(error: String?) = onComplete(false)
        }, description)
    }

    fun addScreenTrack(track: VideoTrack, streamId: String = "approved-screen") {
        checkNotNull(peer) { "Start an approved session first." }
            .addTrack(track, listOf(streamId))
    }

    @Synchronized
    fun close() {
        if (closed) return
        closed = true
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        runCatching { factory.dispose() }
        onStateChanged(WebRtcConnectionState.DISCONNECTED)
    }

    private fun descriptionObserver(onCreated: (SessionDescription?) -> Unit) =
        object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) = onCreated(description)
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) = onCreated(null)
            override fun onSetFailure(error: String?) = Unit
        }
}
