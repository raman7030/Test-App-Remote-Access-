package com.example

import com.example.dpm.EnrollmentPayloadConfig
import com.example.dpm.EnterpriseProvisioningHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun validConfig() = EnrollmentPayloadConfig(
        organizationId = "ORG-DROIDCOMMAND-GLOBAL",
        organizationName = "DroidCommand",
        serverUrl = "https://example.org/enroll",
        enrollmentToken = "test-token",
        signingCertificateChecksum = "a".repeat(64)
    )

    @Test
    fun testEnrollmentConfigValidation() {
        val config = validConfig()
        EnterpriseProvisioningHelper.validate(config)
        assertEquals("ORG-DROIDCOMMAND-GLOBAL", config.organizationId)
        assertTrue(config.leaveAllSystemAppsEnabled)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testQrGenerationRejectsNonStandardPlaceholder() {
        EnterpriseProvisioningHelper.generateQrMatrix("TEST_DATA", size = 29)
    }
}
