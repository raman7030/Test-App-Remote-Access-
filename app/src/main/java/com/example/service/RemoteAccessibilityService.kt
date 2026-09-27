package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.GestureType
import com.example.model.RemoteGesture
import com.example.webrtc.WebRtcSignalingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Accessibility-based remote support input.
 *
 * The user must explicitly enable this service in Android Accessibility settings and
 * separately approve each support session. This service never self-enables or silently
 * grants authorization to an external operator.
 */
class RemoteAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var isServiceActive: Boolean = false
            private set

        @Volatile var isRemoteInteractionAuthorized: Boolean = false
            private set

        @Volatile private var consentedSessionId: String? = null

        /** Called only by the in-app consent flow after showing the operator and scope. */
        fun grantSessionConsent(sessionId: String): Boolean {
            if (sessionId.isBlank()) return false
            consentedSessionId = sessionId
            isRemoteInteractionAuthorized = true
            return true
        }

        /** End the current approved session immediately. */
        fun revokeSessionConsent() {
            isRemoteInteractionAuthorized = false
            consentedSessionId = null
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var gestureJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
        startGestureListener()
    }

    private fun startGestureListener() {
        gestureJob?.cancel()
        gestureJob = serviceScope.launch {
            WebRtcSignalingManager.incomingGestureEvents.collectLatest { gesture ->
                val sessionId = consentedSessionId
                if (isServiceActive && isRemoteInteractionAuthorized &&
                    sessionId != null && WebRtcSignalingManager.isRemoteInputAuthorized(sessionId)
                ) {
                    handleGesture(gesture)
                }
            }
        }
    }

    private fun handleGesture(gesture: RemoteGesture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        when (gesture.type) {
            GestureType.TAP -> dispatchPath(gesture.x, gesture.y, 60L)
            GestureType.LONG_PRESS -> dispatchPath(gesture.x, gesture.y, 600L)
            GestureType.SWIPE -> {
                val path = Path().apply {
                    moveTo(gesture.x, gesture.y)
                    lineTo(gesture.endX, gesture.endY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, 300)
                dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
            }
            GestureType.GLOBAL_ACTION -> {
                // Restrict global actions to documented navigation actions.
                if (gesture.actionCode in listOf(
                        GLOBAL_ACTION_BACK, GLOBAL_ACTION_HOME, GLOBAL_ACTION_RECENTS,
                        GLOBAL_ACTION_NOTIFICATIONS, GLOBAL_ACTION_QUICK_SETTINGS
                    )
                ) performGlobalAction(gesture.actionCode)
            }
            GestureType.TEXT_INPUT -> {
                val text = gesture.textPayload?.take(500) ?: return
                if (text.isNotBlank()) {
                    val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val args = android.os.Bundle().apply {
                            putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                text
                            )
                        }
                        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        focused.recycle()
                    }
                }
            }
        }
    }

    private fun dispatchPath(x: Float, y: Float, durationMs: Long) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen text, notifications, passwords, or UI content are collected.
    }

    override fun onInterrupt() {
        revokeSessionConsent()
    }

    override fun onDestroy() {
        isServiceActive = false
        revokeSessionConsent()
        gestureJob?.cancel()
        super.onDestroy()
    }
}
