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

class RemoteAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceActive: Boolean = false
            private set
        var isRemoteInteractionAuthorized: Boolean = false
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
                if (isRemoteInteractionAuthorized) {
                    handleGesture(gesture)
                }
            }
        }
    }

    private fun handleGesture(gesture: RemoteGesture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        when (gesture.type) {
            GestureType.TAP -> {
                val path = Path().apply {
                    moveTo(gesture.x, gesture.y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, 60)
                val description = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(description, null, null)
            }
            GestureType.LONG_PRESS -> {
                val path = Path().apply {
                    moveTo(gesture.x, gesture.y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, 600)
                val description = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(description, null, null)
            }
            GestureType.SWIPE -> {
                val path = Path().apply {
                    moveTo(gesture.x, gesture.y)
                    lineTo(gesture.endX, gesture.endY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, 300)
                val description = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(description, null, null)
            }
            GestureType.GLOBAL_ACTION -> {
                if (gesture.actionCode != 0) {
                    performGlobalAction(gesture.actionCode)
                }
            }
            GestureType.TEXT_INPUT -> {
                if (!gesture.textPayload.isNullOrBlank()) {
                    val root = rootInActiveWindow ?: return
                    val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val args = android.os.Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, gesture.textPayload)
                        }
                        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility observation if needed for node mapping
    }

    override fun onInterrupt() {
        isServiceActive = false
    }

    override fun onDestroy() {
        isServiceActive = false
        gestureJob?.cancel()
        super.onDestroy()
    }
}
