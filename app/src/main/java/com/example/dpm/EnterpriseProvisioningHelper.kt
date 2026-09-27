package com.example.dpm

import android.content.Context
import org.json.JSONObject

/**
 * Provisioning data builder.
 *
 * Real Android Enterprise provisioning must use a valid, short-lived enrollment token,
 * the APK's actual signing certificate checksum, and an HTTPS endpoint operated by the
 * organization. No credentials or Wi-Fi secrets are embedded in this application.
 */
data class EnrollmentPayloadConfig(
    val organizationId: String,
    val organizationName: String,
    val serverUrl: String,
    val enrollmentToken: String,
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurityType: String = "WPA",
    val isWifiHidden: Boolean = false,
    val leaveAllSystemAppsEnabled: Boolean = true,
    val signingCertificateChecksum: String
)

object EnterpriseProvisioningHelper {

    fun validate(config: EnrollmentPayloadConfig) {
        require(config.organizationId.isNotBlank()) { "Organization ID is required" }
        require(config.organizationName.isNotBlank()) { "Organization name is required" }
        require(config.serverUrl.startsWith("https://", ignoreCase = true)) {
            "Use an HTTPS enrollment endpoint"
        }
        require(config.enrollmentToken.isNotBlank()) { "A server-issued enrollment token is required" }
        require(config.signingCertificateChecksum.matches(Regex("(?i)^[a-f0-9]{64}$"))) {
            "Provide the SHA-256 checksum of the APK signing certificate"
        }
        require(config.wifiSsid.isNotBlank() || config.wifiPassword.isBlank()) {
            "A Wi-Fi password cannot be supplied without an SSID"
        }
    }

    /**
     * Builds Android Enterprise provisioning extras only from explicitly supplied,
     * server-issued configuration. The caller must provide the real signing checksum.
     */
    fun generateEnterpriseProvisioningJson(context: Context, config: EnrollmentPayloadConfig): String {
        validate(config)
        val packageName = context.packageName
        val adminComponent = "$packageName/com.example.receiver.EnterpriseDeviceAdminReceiver"

        val adminExtras = JSONObject().apply {
            put("enrollment_token", config.enrollmentToken)
            put("org_id", config.organizationId)
            put("org_name", config.organizationName)
            put("server_endpoint", config.serverUrl)
            put("enrolled_at", System.currentTimeMillis())
        }

        val json = JSONObject().apply {
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME", packageName)
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", adminComponent)
            put("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM", config.signingCertificateChecksum)
            put("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", adminExtras)
            put("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", config.leaveAllSystemAppsEnabled)
            put("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", false)
            if (config.wifiSsid.isNotBlank()) {
                put("android.app.extra.PROVISIONING_WIFI_SSID", config.wifiSsid)
                if (config.wifiPassword.isNotBlank()) {
                    put("android.app.extra.PROVISIONING_WIFI_PASSWORD", config.wifiPassword)
                }
                put("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", config.wifiSecurityType)
                put("android.app.extra.PROVISIONING_WIFI_HIDDEN", config.isWifiHidden)
            }
        }
        return json.toString(2)
    }

    /**
     * Intentionally does not synthesize QR-like pixels. Hash-pattern matrices are not
     * valid QR codes. Use a standards-compliant QR encoder on the validated JSON payload.
     */
    @Deprecated("Use a standards-compliant QR encoder with validated provisioning JSON")
    fun generateQrMatrix(data: String, size: Int = 29): Array<BooleanArray> {
        require(data.isNotBlank()) { "QR payload must not be blank" }
        require(size >= 21) { "QR matrix size is too small" }
        throw UnsupportedOperationException(
            "This helper does not generate standards-compliant QR codes. Encode the validated provisioning JSON with a QR library."
        )
    }
}
