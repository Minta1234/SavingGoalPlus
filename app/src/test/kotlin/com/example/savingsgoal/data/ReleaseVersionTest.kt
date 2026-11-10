package com.example.savingsgoal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun comparesNumericComponentsRatherThanText() {
        assertEquals(-1, compareReleaseVersions("2.0.9", "v2.0.10"))
        assertEquals(0, compareReleaseVersions("2.0.10", "v2.0.10"))
        assertEquals(1, compareReleaseVersions("2.1.0", "v2.0.99"))
        assertNull(compareReleaseVersions("2.1", "v2.1.0"))
        assertNull(compareReleaseVersions("2.1.0", "v2.1.0-beta"))
    }
}
