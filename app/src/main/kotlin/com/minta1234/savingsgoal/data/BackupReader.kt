package com.minta1234.savingsgoal.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs

internal object BackupReader {
    private const val MAX_BYTES = 16L * 1024 * 1024
    private val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun copy(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            require(total <= MAX_BYTES) { "Backup is larger than 16 MB" }
            output.write(buffer, 0, read)
        }
        require(total > 0) { "Backup is empty" }
    }

    fun read(file: File): BackupPayload {
        val header = file.inputStream().buffered().use { input ->
            val bytes = ByteArray(sqliteHeader.size)
            val count = input.read(bytes)
            count == bytes.size && bytes.contentEquals(sqliteHeader)
        }
        return if (header) readSqlite(file) else readJson(file.readText(Charsets.UTF_8))
    }

    fun readJson(text: String): BackupPayload {
        val root = Json.parseToJsonElement(text.trimStart().removePrefix("\uFEFF"))
        // Older Android exports omit fields whose value equals the schema default.
        if (root is JsonObject && "version" in root) {
            require(root.field("version")?.longOrNull in listOf(1L, 2L)) { "Unsupported backup version" }
        }
        val androidExport = root is JsonObject &&
            root.keys.all { it in setOf("version", "exportDate", "goals", "payments") } &&
            root.field("exportDate")?.contentOrNull?.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) == true
        fun array(source: JsonObject, key: String): JsonArray {
            val element = source[key]
            if (element == null && androidExport) return JsonArray(emptyList())
            return element as? JsonArray ?: error("Missing or invalid $key array")
        }
        val goals = when (root) {
            is JsonObject -> array(root, "goals")
            is JsonArray -> root.also { require(it.isNotEmpty()) { "Unrecognized empty list" } }
            else -> error("Unrecognized backup")
        }
        val payments = if (root is JsonObject) array(root, "payments") else JsonArray(emptyList())
        return checked(
            goals.map { row ->
                require(row is JsonObject) { "Invalid goal record" }
                val title = row.field("name", "title")?.contentOrNull ?: error("Missing goal name")
                val target = row.field("amount", "targetAmount")?.doubleOrNull
                    ?: if (androidExport && "amount" !in row) 0.0 else error("Invalid target amount")
                Goal(
                    id = row.field("id")?.longOrNull ?: 0,
                    name = title,
                    amount = target,
                    date = row.field("date", "targetDate")?.contentOrNull,
                    folder = row.field("folder")?.contentOrNull
                        ?: if (row.field("isArchived")?.booleanOrNull == true) "ARCHIVE" else "MAIN",
                    position = row.field("position")?.longOrNull?.toInt() ?: 0,
                    note = row.field("note", "notes")?.contentOrNull.orEmpty(),
                    currency = row.field("currency")?.contentOrNull
                )
            },
            payments.map { row ->
                require(row is JsonObject) { "Invalid payment record" }
                val amount = row.field("amount")?.doubleOrNull
                    ?: if (androidExport && "amount" !in row) 0.0 else error("Invalid payment amount")
                Payment(
                    id = row.field("id")?.longOrNull ?: 0,
                    mainId = row.field("mainId", "goalId")?.longOrNull ?: error("Missing payment goal"),
                    date = row.field("date")?.contentOrNull.orEmpty(),
                    amount = abs(amount),
                    isSaving = row.field("isSaving")?.booleanOrNull ?: (amount >= 0),
                    note = row.field("note", "notes")?.contentOrNull.orEmpty()
                )
            }
        )
    }

    private fun readSqlite(file: File): BackupPayload =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val goals = db.rawQuery("SELECT * FROM MAINLIST", null).use { cursor ->
                cursor.requireColumns("_id", "NAME", "AMOUNT")
                buildList {
                    while (cursor.moveToNext()) add(Goal(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                        name = cursor.optional("NAME").orEmpty(),
                        amount = cursor.getDouble(cursor.getColumnIndexOrThrow("AMOUNT")),
                        date = cursor.optional("DATE"),
                        folder = cursor.optional("FOLDER") ?: "MAIN",
                        position = cursor.optional("POSITION")?.toIntOrNull() ?: 0,
                        note = cursor.optional("NOTE").orEmpty(),
                        currency = cursor.optional("CURRENCY")
                    ))
                }
            }
            val payments = db.rawQuery("SELECT * FROM PAYMENTLIST", null).use { cursor ->
                cursor.requireColumns("_id", "MAINID", "AMOUNT")
                buildList {
                    while (cursor.moveToNext()) {
                        val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("AMOUNT"))
                        val type = cursor.optional("DOSAVING")
                        add(Payment(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                            mainId = cursor.getLong(cursor.getColumnIndexOrThrow("MAINID")),
                            date = cursor.optional("DATE").orEmpty(),
                            amount = abs(amount),
                            isSaving = type?.let { it.equals("true", true) || it == "1" } ?: (amount >= 0),
                            note = cursor.optional("NOTE").orEmpty()
                        ))
                    }
                }
            }
            checked(goals, payments)
        }

    private fun checked(goals: List<Goal>, payments: List<Payment>): BackupPayload {
        val goalIds = goals.map { it.id }.filter { it > 0 }
        val paymentIds = payments.map { it.id }.filter { it > 0 }
        val validGoals = goalIds.toSet()
        require(validGoals.size == goalIds.size && paymentIds.toSet().size == paymentIds.size) { "Duplicate IDs" }
        require(goals.all { it.id in 0 until Long.MAX_VALUE && it.name.isNotBlank() &&
            it.amount.isFinite() && it.amount >= 0 && it.folder in listOf("MAIN", "ARCHIVE") }) { "Invalid goal" }
        require(payments.all { it.id in 0 until Long.MAX_VALUE &&
            it.mainId in validGoals && it.amount.isFinite() }) { "Invalid transaction or missing goal" }
        return BackupPayload(goals = goals, payments = payments)
    }

    private fun JsonObject.field(vararg names: String): JsonPrimitive? =
        names.firstNotNullOfOrNull { this[it] as? JsonPrimitive }

    private fun Cursor.optional(name: String): String? =
        getColumnIndex(name).takeIf { it >= 0 }?.let(::getString)

    private fun Cursor.requireColumns(vararg names: String) {
        names.forEach { getColumnIndexOrThrow(it) }
    }
}
