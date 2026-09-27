package com.example.dpm

import android.content.Context
import org.json.JSONObject

data class EnrollmentPayloadConfig(
    val organizationId: String = "ORG-DROIDCOMMAND-GLOBAL",
    val organizationName: String = "Enterprise Headquarters",
    val serverUrl: String = "https://mdm.droidcommand.enterprise.internal/api/v2",
    val enrollmentToken: String = "TKN-SECURE-9921-X",
    val wifiSsid: String = "Corp-Secure-Net",
    val wifiPassword: String = "CorpPass2026!",
    val wifiSecurityType: String = "WPA",
    val isWifiHidden: Boolean = false,
    val leaveAllSystemAppsEnabled: Boolean = true
)

object EnterpriseProvisioningHelper {

    fun generateEnterpriseProvisioningJson(context: Context, config: EnrollmentPayloadConfig): String {
        val packageName = context.packageName
        val adminComponent = "$packageName/com.example.receiver.EnterpriseDeviceAdminReceiver"

        val adminExtras = JSONObject().apply {
            put("enrollment_token", config.enrollmentToken)
            put("org_id", config.organizationId)
            put("org_name", config.organizationName)
            put("server_endpoint", config.serverUrl)
            put("enrolled_by", "admin@droidcommand.ultimate")
            put("enrolled_at", System.currentTimeMillis())
        }

        val json = JSONObject().apply {
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME", packageName)
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", adminComponent)
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM", "DCU99824A1F6E482C310")
            put("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", adminExtras)
            put("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", config.leaveAllSystemAppsEnabled)
            put("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", false)
            if (config.wifiSsid.isNotBlank()) {
                put("android.app.extra.PROVISIONING_WIFI_SSID", config.wifiSsid)
                put("android.app.extra.PROVISIONING_WIFI_PASSWORD", config.wifiPassword)
                put("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", config.wifiSecurityType)
                put("android.app.extra.PROVISIONING_WIFI_HIDDEN", config.isWifiHidden)
            }
        }

        return json.toString(2)
    }

    /**
     * Generates a boolean 2D matrix representing QR-code like patterns
     * based on string data hashing for rendering on Jetpack Compose Canvas.
     */
    fun generateQrMatrix(data: String, size: Int = 29): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) }
        val bytes = data.toByteArray(Charsets.UTF_8)
        var byteIdx = 0

        // 1. Finder patterns (7x7 top-left, top-right, bottom-left)
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = (r == 0 || r == 6 || c == 0 || c == 6)
                    val isCenter = (r in 2..4 && c in 2..4)
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(size - 7, 0)
        drawFinder(0, size - 7)

        // 2. Timing patterns
        for (i in 7 until size - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // 3. Data modules using byte hash
        for (r in 0 until size) {
            for (c in 0 until size) {
                // skip finder patterns
                val inTopLeft = (r < 8 && c < 8)
                val inTopRight = (r < 8 && c >= size - 8)
                val inBottomLeft = (r >= size - 8 && c < 8)
                val isTiming = (r == 6 || c == 6)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !isTiming) {
                    val b = if (bytes.isNotEmpty()) bytes[byteIdx % bytes.size].toInt() else 0
                    val bit = ((b xor (r * 31 + c * 17)) and 0x01) == 1
                    matrix[r][c] = bit
                    byteIdx++
                }
            }
        }

        return matrix
    }
}
