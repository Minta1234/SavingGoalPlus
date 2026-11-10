package com.minta1234.savingsgoal.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.minta1234.savingsgoal.widget.SavingsGoalWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class SavingsRepository(private val context: Context) {

    private val dbHelper = SavingsGoalDbHelper.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY",
        Context.MODE_PRIVATE
    )

    private val _dataRevision = MutableStateFlow(0L)
    val dataRevision: StateFlow<Long> = _dataRevision.asStateFlow()

    fun notifyChanged() {
        _dataRevision.value += 1
        try {
            SavingsGoalWidgetProvider.triggerUpdate(context)
        } catch (_: Exception) {
        }
    }

    // --- Preferences ---

    fun getDefaultCurrency(): String {
        return prefs.getString("CURRENCY_SETTING", "$") ?: "$"
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString("CURRENCY_SETTING", currency).apply()
        notifyChanged()
    }

    fun getToSaveOption(): Int {
        // 1 = Weekly, 2 = Bi-Weekly
        return prefs.getInt("TO_SAVE_OPTION", 1)
    }

    fun setToSaveOption(option: Int) {
        prefs.edit().putInt("TO_SAVE_OPTION", option).apply()
        notifyChanged()
    }

    fun getShowTotalGoalsCard(): Boolean {
        return prefs.getBoolean("MAIN_TOTAL_GOAL", true)
    }

    fun setShowTotalGoalsCard(show: Boolean) {
        prefs.edit().putBoolean("MAIN_TOTAL_GOAL", show).apply()
        notifyChanged()
    }

    // --- Goals CRUD ---

    suspend fun getActiveGoals(): List<Goal> = withContext(Dispatchers.IO) {
        getGoalsByFolder("MAIN")
    }

    suspend fun getArchivedGoals(): List<Goal> = withContext(Dispatchers.IO) {
        getGoalsByFolder("ARCHIVE")
    }

    private fun getGoalsByFolder(folder: String): List<Goal> {
        val db = dbHelper.readableDatabase
        val goals = mutableListOf<Goal>()

        val cursor = db.query(
            SavingsGoalDbHelper.TABLE_MAINLIST,
            null,
            "${SavingsGoalDbHelper.COL_FOLDER} = ?",
            arrayOf(folder),
            null,
            null,
            "${SavingsGoalDbHelper.COL_POSITION} ASC, ${SavingsGoalDbHelper.COL_ID} DESC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val goal = cursorToGoal(c)
                goals.add(goal)
            }
        }
        return goals
    }

    suspend fun getGoalById(id: Long): Goal? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SavingsGoalDbHelper.TABLE_MAINLIST,
            null,
            "${SavingsGoalDbHelper.COL_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use { c ->
            if (c.moveToFirst()) cursorToGoal(c) else null
        }
    }

    private fun cursorToGoal(c: Cursor): Goal {
        val id = c.getLong(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_ID))
        val name = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_NAME)) ?: "untitled"
        val amount = c.getDouble(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_AMOUNT))
        val date = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_DATE))
        val folder = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_FOLDER)) ?: "MAIN"
        val position = c.getInt(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_POSITION))
        val note = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_NOTE)) ?: ""
        val currency = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_CURRENCY))

        // Check image file existence
        val imageFile = File(context.filesDir, id.toString())
        val imagePath = if (imageFile.exists() && imageFile.length() > 0) imageFile.absolutePath else null

        // Calculate total saved and payment count
        val (totalSaved, count) = getPaymentTotalsForGoal(id)

        return Goal(
            id = id,
            name = name,
            amount = amount,
            date = date,
            folder = folder,
            position = position,
            note = note,
            currency = currency,
            imagePath = imagePath,
            totalSaved = totalSaved,
            paymentCount = count
        )
    }

    private fun getPaymentTotalsForGoal(goalId: Long): Pair<Double, Int> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SavingsGoalDbHelper.TABLE_PAYMENTLIST,
            arrayOf(SavingsGoalDbHelper.COL_PAY_AMOUNT, SavingsGoalDbHelper.COL_PAY_DOSAVING),
            "${SavingsGoalDbHelper.COL_PAY_MAINID} = ?",
            arrayOf(goalId.toString()),
            null,
            null,
            null
        )
        var total = 0.0
        var count = 0
        cursor.use { c ->
            while (c.moveToNext()) {
                val amt = c.getDouble(0)
                val isSavingStr = c.getString(1)
                val isSaving = isSavingStr.equals("true", ignoreCase = true)
                if (isSaving) {
                    total += amt
                } else {
                    total -= amt
                }
                count++
            }
        }
        return Pair(total, count)
    }

    suspend fun insertGoal(
        name: String,
        amount: Double,
        date: String?,
        note: String,
        currency: String?,
        imageBitmap: Bitmap?
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SavingsGoalDbHelper.COL_NAME, name.ifBlank { "untitled" })
            put(SavingsGoalDbHelper.COL_AMOUNT, amount)
            put(SavingsGoalDbHelper.COL_DATE, date)
            put(SavingsGoalDbHelper.COL_FOLDER, "MAIN")
            put(SavingsGoalDbHelper.COL_POSITION, 0)
            put(SavingsGoalDbHelper.COL_NOTE, note)
            put(SavingsGoalDbHelper.COL_CURRENCY, currency ?: "null")
        }
        val id = db.insertOrThrow(SavingsGoalDbHelper.TABLE_MAINLIST, null, values)

        if (imageBitmap != null) {
            saveGoalImage(id, imageBitmap)
        }
        notifyChanged()
        id
    }

    suspend fun updateGoal(
        id: Long,
        name: String,
        amount: Double,
        date: String?,
        note: String,
        currency: String?,
        imageBitmap: Bitmap? = null,
        removeImage: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SavingsGoalDbHelper.COL_NAME, name.ifBlank { "untitled" })
            put(SavingsGoalDbHelper.COL_AMOUNT, amount)
            put(SavingsGoalDbHelper.COL_DATE, date)
            put(SavingsGoalDbHelper.COL_NOTE, note)
            put(SavingsGoalDbHelper.COL_CURRENCY, currency ?: "null")
        }
        db.update(
            SavingsGoalDbHelper.TABLE_MAINLIST,
            values,
            "${SavingsGoalDbHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )

        if (removeImage) {
            deleteGoalImage(id)
        } else if (imageBitmap != null) {
            saveGoalImage(id, imageBitmap)
        }
        notifyChanged()
    }

    suspend fun setGoalFolder(id: Long, folder: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SavingsGoalDbHelper.COL_FOLDER, folder)
        }
        db.update(
            SavingsGoalDbHelper.TABLE_MAINLIST,
            values,
            "${SavingsGoalDbHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        notifyChanged()
    }

    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            SavingsGoalDbHelper.TABLE_MAINLIST,
            "${SavingsGoalDbHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        db.delete(
            SavingsGoalDbHelper.TABLE_PAYMENTLIST,
            "${SavingsGoalDbHelper.COL_PAY_MAINID} = ?",
            arrayOf(id.toString())
        )
        deleteGoalImage(id)
        notifyChanged()
    }

    suspend fun updateGoalPositions(orderedGoalIds: List<Long>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            orderedGoalIds.forEachIndexed { index, id ->
                val cv = ContentValues().apply {
                    put(SavingsGoalDbHelper.COL_POSITION, index)
                }
                db.update(
                    SavingsGoalDbHelper.TABLE_MAINLIST,
                    cv,
                    "${SavingsGoalDbHelper.COL_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    // --- Image Handling ---

    private fun saveGoalImage(id: Long, bitmap: Bitmap) {
        try {
            val file = File(context.filesDir, id.toString())
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) {}
    }

    private fun deleteGoalImage(id: Long) {
        try {
            val file = File(context.filesDir, id.toString())
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    // --- Payments CRUD ---

    suspend fun getPaymentsForGoal(goalId: Long, sortOrder: SortOrder): List<Payment> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val orderBy = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> "${SavingsGoalDbHelper.COL_PAY_DATE} DESC, ${SavingsGoalDbHelper.COL_PAY_ID} DESC"
            SortOrder.OLDEST_FIRST -> "${SavingsGoalDbHelper.COL_PAY_DATE} ASC, ${SavingsGoalDbHelper.COL_PAY_ID} ASC"
            SortOrder.HIGHEST_AMOUNT -> "${SavingsGoalDbHelper.COL_PAY_AMOUNT} DESC, ${SavingsGoalDbHelper.COL_PAY_ID} DESC"
        }

        val payments = mutableListOf<Payment>()
        val cursor = db.query(
            SavingsGoalDbHelper.TABLE_PAYMENTLIST,
            null,
            "${SavingsGoalDbHelper.COL_PAY_MAINID} = ?",
            arrayOf(goalId.toString()),
            null,
            null,
            orderBy
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_ID))
                val mainId = c.getLong(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_MAINID))
                val date = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_DATE)) ?: LocalDate.now().toString()
                val amount = c.getDouble(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_AMOUNT))
                val isSavingStr = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_DOSAVING))
                val isSaving = isSavingStr.equals("true", ignoreCase = true)
                val note = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_NOTE)) ?: ""

                payments.add(
                    Payment(
                        id = id,
                        mainId = mainId,
                        date = date,
                        amount = amount,
                        isSaving = isSaving,
                        note = note
                    )
                )
            }
        }
        payments
    }

    suspend fun insertPayment(
        goalId: Long,
        amount: Double,
        date: String,
        isSaving: Boolean,
        note: String
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(SavingsGoalDbHelper.COL_PAY_MAINID, goalId)
            put(SavingsGoalDbHelper.COL_PAY_AMOUNT, amount)
            put(SavingsGoalDbHelper.COL_PAY_DATE, date)
            put(SavingsGoalDbHelper.COL_PAY_DOSAVING, isSaving.toString())
            put(SavingsGoalDbHelper.COL_PAY_NOTE, note)
        }
        val id = db.insertOrThrow(SavingsGoalDbHelper.TABLE_PAYMENTLIST, null, cv)
        if (isSaving && amount > 0) {
            SavingsStreakManager.recordDepositStreak(context)
        }
        notifyChanged()
        id
    }

    suspend fun updatePayment(
        id: Long,
        amount: Double,
        date: String,
        isSaving: Boolean,
        note: String
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(SavingsGoalDbHelper.COL_PAY_AMOUNT, amount)
            put(SavingsGoalDbHelper.COL_PAY_DATE, date)
            put(SavingsGoalDbHelper.COL_PAY_DOSAVING, isSaving.toString())
            put(SavingsGoalDbHelper.COL_PAY_NOTE, note)
        }
        db.update(
            SavingsGoalDbHelper.TABLE_PAYMENTLIST,
            cv,
            "${SavingsGoalDbHelper.COL_PAY_ID} = ?",
            arrayOf(id.toString())
        )
        notifyChanged()
    }

    suspend fun deletePayment(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            SavingsGoalDbHelper.TABLE_PAYMENTLIST,
            "${SavingsGoalDbHelper.COL_PAY_ID} = ?",
            arrayOf(id.toString())
        )
        notifyChanged()
    }

    // --- Overview Summary ---

    suspend fun getTotalSummary(): TotalSummary = withContext(Dispatchers.IO) {
        val activeGoals = getActiveGoals()
        var totalTarget = 0.0
        var totalSaved = 0.0
        var totalRemaining = 0.0

        for (goal in activeGoals) {
            totalTarget += goal.amount
            totalSaved += goal.totalSaved
            totalRemaining += goal.remainingAmount
        }

        TotalSummary(
            totalGoals = activeGoals.size,
            totalTarget = totalTarget,
            totalSaved = totalSaved,
            totalRemaining = totalRemaining
        )
    }

    // --- Backup & Export ---

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val allGoals = getGoalsByFolder("MAIN") + getGoalsByFolder("ARCHIVE")
        val allPayments = mutableListOf<Payment>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(SavingsGoalDbHelper.TABLE_PAYMENTLIST, null, null, null, null, null, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_ID))
                val mainId = c.getLong(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_MAINID))
                val date = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_DATE)) ?: ""
                val amount = c.getDouble(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_AMOUNT))
                val isSavingStr = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_DOSAVING))
                val isSaving = isSavingStr.equals("true", ignoreCase = true)
                val note = c.getString(c.getColumnIndexOrThrow(SavingsGoalDbHelper.COL_PAY_NOTE)) ?: ""
                allPayments.add(Payment(id, mainId, date, amount, isSaving, note))
            }
        }
        val payload = BackupPayload(
            exportDate = LocalDate.now().toString(),
            goals = allGoals,
            payments = allPayments
        )
        val json = Json { prettyPrint = true }
        json.encodeToString(payload)
    }

    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanJson = jsonString.trim().removePrefix("\uFEFF")
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            val parsedGoals = mutableListOf<Goal>()
            val parsedPayments = mutableListOf<Payment>()

            try {
                val payload = json.decodeFromString<BackupPayload>(cleanJson)
                parsedGoals.addAll(payload.goals)
                parsedPayments.addAll(payload.payments)
            } catch (e: Exception) {
                android.util.Log.w("SavingsRepository", "Direct BackupPayload decode failed, trying dynamic JSON parse", e)
            }

            // Fallback for iOS format or raw array
            if (parsedGoals.isEmpty()) {
                val element = json.parseToJsonElement(cleanJson)
                if (element is JsonObject) {
                    val goalsArray = element["goals"] as? JsonArray
                    goalsArray?.forEach { gElem ->
                        if (gElem is JsonObject) {
                            val id = gElem["id"]?.jsonPrimitive?.longOrNull ?: 0L
                            val name = gElem["name"]?.jsonPrimitive?.contentOrNull
                                ?: gElem["title"]?.jsonPrimitive?.contentOrNull
                                ?: "Goal"
                            val amount = gElem["amount"]?.jsonPrimitive?.doubleOrNull
                                ?: gElem["targetAmount"]?.jsonPrimitive?.doubleOrNull
                                ?: 0.0
                            val date = gElem["date"]?.jsonPrimitive?.contentOrNull
                                ?: gElem["targetDate"]?.jsonPrimitive?.contentOrNull
                            val folder = gElem["folder"]?.jsonPrimitive?.contentOrNull
                                ?: if (gElem["isArchived"]?.jsonPrimitive?.booleanOrNull == true) "ARCHIVE" else "MAIN"
                            val note = gElem["note"]?.jsonPrimitive?.contentOrNull
                                ?: gElem["notes"]?.jsonPrimitive?.contentOrNull
                                ?: ""
                            val currency = gElem["currency"]?.jsonPrimitive?.contentOrNull
                            parsedGoals.add(
                                Goal(
                                    id = id,
                                    name = name,
                                    amount = amount,
                                    date = date,
                                    folder = folder,
                                    note = note,
                                    currency = currency
                                )
                            )
                        }
                    }

                    val paymentsArray = element["payments"] as? JsonArray
                    paymentsArray?.forEach { pElem ->
                        if (pElem is JsonObject) {
                            val id = pElem["id"]?.jsonPrimitive?.longOrNull ?: 0L
                            val mainId = pElem["mainId"]?.jsonPrimitive?.longOrNull
                                ?: pElem["goalId"]?.jsonPrimitive?.longOrNull
                                ?: 0L
                            val date = pElem["date"]?.jsonPrimitive?.contentOrNull ?: LocalDate.now().toString()
                            val amount = pElem["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val isSaving = pElem["isSaving"]?.jsonPrimitive?.booleanOrNull ?: true
                            val note = pElem["note"]?.jsonPrimitive?.contentOrNull
                                ?: pElem["notes"]?.jsonPrimitive?.contentOrNull
                                ?: ""
                            parsedPayments.add(
                                Payment(
                                    id = id,
                                    mainId = mainId,
                                    date = date,
                                    amount = amount,
                                    isSaving = isSaving,
                                    note = note
                                )
                            )
                        }
                    }
                } else if (element is JsonArray) {
                    element.forEach { gElem ->
                        if (gElem is JsonObject) {
                            val id = gElem["id"]?.jsonPrimitive?.longOrNull ?: 0L
                            val name = gElem["name"]?.jsonPrimitive?.contentOrNull
                                ?: gElem["title"]?.jsonPrimitive?.contentOrNull
                                ?: "Goal"
                            val amount = gElem["amount"]?.jsonPrimitive?.doubleOrNull
                                ?: gElem["targetAmount"]?.jsonPrimitive?.doubleOrNull
                                ?: 0.0
                            parsedGoals.add(Goal(id = id, name = name, amount = amount))
                        }
                    }
                }
            }

            if (parsedGoals.isEmpty() && parsedPayments.isEmpty()) {
                android.util.Log.e("SavingsRepository", "No valid goals or payments found in JSON")
                return@withContext false
            }

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(SavingsGoalDbHelper.TABLE_MAINLIST, null, null)
                db.delete(SavingsGoalDbHelper.TABLE_PAYMENTLIST, null, null)

                parsedGoals.forEach { g ->
                    val cv = ContentValues().apply {
                        if (g.id > 0) put(SavingsGoalDbHelper.COL_ID, g.id)
                        put(SavingsGoalDbHelper.COL_NAME, g.name)
                        put(SavingsGoalDbHelper.COL_AMOUNT, g.amount)
                        put(SavingsGoalDbHelper.COL_DATE, g.date)
                        put(SavingsGoalDbHelper.COL_FOLDER, g.folder)
                        put(SavingsGoalDbHelper.COL_POSITION, g.position)
                        put(SavingsGoalDbHelper.COL_NOTE, g.note)
                        put(SavingsGoalDbHelper.COL_CURRENCY, g.currency ?: "null")
                    }
                    db.insertWithOnConflict(
                        SavingsGoalDbHelper.TABLE_MAINLIST,
                        null,
                        cv,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                }

                parsedPayments.forEach { p ->
                    val cv = ContentValues().apply {
                        if (p.id > 0) put(SavingsGoalDbHelper.COL_PAY_ID, p.id)
                        put(SavingsGoalDbHelper.COL_PAY_MAINID, p.mainId)
                        put(SavingsGoalDbHelper.COL_PAY_DATE, p.date)
                        put(SavingsGoalDbHelper.COL_PAY_AMOUNT, p.amount)
                        put(SavingsGoalDbHelper.COL_PAY_DOSAVING, p.isSaving.toString())
                        put(SavingsGoalDbHelper.COL_PAY_NOTE, p.note)
                    }
                    db.insertWithOnConflict(
                        SavingsGoalDbHelper.TABLE_PAYMENTLIST,
                        null,
                        cv,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            notifyChanged()
            true
        } catch (e: Exception) {
            android.util.Log.e("SavingsRepository", "Error during importFromJson", e)
            false
        }
    }

    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val activeGoals = getActiveGoals()
        val sb = StringBuilder()
        sb.append("Goal ID,Goal Name,Target Amount,Saved Amount,Remaining,Target Date,Folder,Notes\n")
        for (g in activeGoals) {
            sb.append("${g.id},\"${g.name.replace("\"", "\"\"")}\",${g.amount},${g.totalSaved},${g.remainingAmount},\"${g.date ?: ""}\",${g.folder},\"${g.note.replace("\"", "\"\"")}\"\n")
        }
        sb.toString()
    }
}
