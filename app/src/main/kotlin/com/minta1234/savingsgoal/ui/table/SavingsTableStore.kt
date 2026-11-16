package com.minta1234.savingsgoal.ui.table

import android.content.SharedPreferences
import com.minta1234.savingsgoal.data.SavingsTableEntry
import com.minta1234.savingsgoal.data.SavingsTablePlan
import org.json.JSONArray
import org.json.JSONObject

internal class SavingsTableStore(private val prefs: SharedPreferences) {
    fun load(legacyName: String): List<SavingsTableEntry> {
        if (prefs.contains("tables_v2")) {
            val array = JSONArray(prefs.getString("tables_v2", "[]"))
            return (0 until array.length()).map { i ->
                val value = array.getJSONObject(i)
                val days = value.getJSONArray("completed")
                SavingsTableEntry(
                    id = value.getString("id"), name = value.getString("name"),
                    plan = SavingsTablePlan(value.getLong("daily"), value.getLong("target")),
                    completed = (0 until days.length()).map { days.getInt(it) }.toSet()
                )
            }
        }
        val legacy = runCatching {
            SavingsTableEntry(name = legacyName,
                plan = SavingsTablePlan(prefs.getLong("daily", 0), prefs.getLong("target", 0)),
                completed = prefs.getStringSet("completed", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet())
        }.getOrNull()
        val migrated = listOfNotNull(legacy)
        save(migrated)
        return migrated
    }

    fun save(tables: List<SavingsTableEntry>) {
        val array = JSONArray()
        tables.forEach { table ->
            array.put(JSONObject().put("id", table.id).put("name", table.name)
                .put("daily", table.plan.daily).put("target", table.plan.target)
                .put("completed", JSONArray(table.completed.sorted())))
        }
        prefs.edit().putString("tables_v2", array.toString()).apply()
    }
}
