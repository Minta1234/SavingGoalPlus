package com.minta1234.savingsgoal.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SavingsGoalDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "SAVINGGOALDB"
        const val DATABASE_VERSION = 6

        // Table MAINLIST
        const val TABLE_MAINLIST = "MAINLIST"
        const val COL_ID = "_id"
        const val COL_NAME = "NAME"
        const val COL_AMOUNT = "AMOUNT"
        const val COL_DATE = "DATE"
        const val COL_FOLDER = "FOLDER"
        const val COL_POSITION = "POSITION"
        const val COL_NOTE = "NOTE"
        const val COL_CURRENCY = "CURRENCY"

        // Table PAYMENTLIST
        const val TABLE_PAYMENTLIST = "PAYMENTLIST"
        const val COL_PAY_ID = "_id"
        const val COL_PAY_MAINID = "MAINID"
        const val COL_PAY_DATE = "DATE"
        const val COL_PAY_AMOUNT = "AMOUNT"
        const val COL_PAY_DOSAVING = "DOSAVING" // "true" or "false"
        const val COL_PAY_NOTE = "NOTE"

        @Volatile
        private var instance: SavingsGoalDbHelper? = null

        fun getInstance(context: Context): SavingsGoalDbHelper {
            return instance ?: synchronized(this) {
                instance ?: SavingsGoalDbHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.disableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MAINLIST(
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT,
                $COL_AMOUNT DOUBLE,
                $COL_DATE TEXT,
                $COL_FOLDER TEXT DEFAULT 'MAIN',
                $COL_POSITION INTEGER DEFAULT 0,
                $COL_NOTE TEXT DEFAULT '',
                $COL_CURRENCY TEXT DEFAULT 'null'
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PAYMENTLIST(
                $COL_PAY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PAY_MAINID INTEGER,
                $COL_PAY_DATE TEXT,
                $COL_PAY_AMOUNT DOUBLE,
                $COL_PAY_DOSAVING VARCHAR(1) DEFAULT 'true',
                $COL_PAY_NOTE TEXT DEFAULT ''
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Preserves existing data when upgrading from any older version
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_PAYMENTLIST ADD COLUMN $COL_PAY_NOTE TEXT DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_MAINLIST ADD COLUMN $COL_FOLDER TEXT DEFAULT 'MAIN'")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_MAINLIST ADD COLUMN $COL_NOTE TEXT DEFAULT ''")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE_MAINLIST ADD COLUMN $COL_CURRENCY TEXT DEFAULT 'null'")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TABLE_PAYMENTLIST ADD COLUMN $COL_PAY_DOSAVING VARCHAR(1) DEFAULT 'true'")
        }
    }

    // Direct database file backup
    fun exportDatabaseFile(context: Context, destFile: File): Boolean {
        return try {
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            if (!dbPath.exists()) return false
            FileInputStream(dbPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Direct database file restore
    fun importDatabaseFile(context: Context, srcFile: File): Boolean {
        return try {
            close()
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            FileInputStream(srcFile).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

