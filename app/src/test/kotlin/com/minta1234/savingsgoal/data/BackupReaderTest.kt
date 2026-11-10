package com.minta1234.savingsgoal.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupReaderTest {
    @Test
    fun readsLegacyAndroidBackupWithOmittedDefaultFields() {
        val backup = BackupReader.readJson(
            """{"exportDate":"2026-11-10","goals":[{"id":1,"name":"Trip"}]}"""
        )
        assertEquals("Trip", backup.goals.single().name)
        assertEquals(0.0, backup.goals.single().amount, 0.0)
        assertEquals(0, backup.payments.size)
    }

    @Test
    fun roundTripsEmptyAndPopulatedAndroidExports() {
        val codec = Json { encodeDefaults = true }
        val goal = Goal(id = 1, name = "Trip", amount = 100.0)
        val withdrawal = Payment(id = 1, mainId = 1, amount = 20.0, isSaving = false)
        for (payload in listOf(BackupPayload(), BackupPayload(goals = listOf(goal), payments = listOf(withdrawal)))) {
            val decoded = BackupReader.readJson(codec.encodeToString(payload))
            assertEquals(payload.goals, decoded.goals)
            assertEquals(payload.payments, decoded.payments)
        }
    }

    @Test
    fun preservesSignedWithdrawalsFromIos() {
        val backup = BackupReader.readJson(
            """{"version":1,"goals":[{"id":1,"title":"Trip","targetAmount":100}],
                "payments":[{"id":2,"goalId":1,"amount":-20}]}"""
        )
        assertFalse(backup.payments.single().isSaving)
        assertEquals(20.0, backup.payments.single().amount, 0.0)
    }

    @Test
    fun rejectsUnrelatedJsonAndOrphanedTransactions() {
        assertThrows(IllegalStateException::class.java) { BackupReader.readJson("{}") }
        assertThrows(IllegalArgumentException::class.java) {
            BackupReader.readJson("""{"goals":[],"payments":[{"mainId":99,"amount":5}]}""")
        }
    }
}
