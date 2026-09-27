package com.example

import com.example.dpm.EnrollmentPayloadConfig
import com.example.dpm.EnterpriseProvisioningHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testQrMatrixGeneration() {
        val matrix = EnterpriseProvisioningHelper.generateQrMatrix("TEST_DATA", size = 29)
        assertEquals(29, matrix.size)
        assertEquals(29, matrix[0].size)

        // Top-left finder corner (0,0) should be true
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
    }

    @Test
    fun testEnrollmentConfigDefaultValues() {
        val config = EnrollmentPayloadConfig()
        assertEquals("ORG-DROIDCOMMAND-GLOBAL", config.organizationId)
        assertTrue(config.wifiSsid.isNotBlank())
        assertTrue(config.leaveAllSystemAppsEnabled)
    }
}
