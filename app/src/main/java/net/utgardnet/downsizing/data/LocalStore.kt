package net.utgardnet.downsizing.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

data class WeightEntry(val id: Long, val day: String, val kg: Double)
data class FoodEntry(val id: Long, val day: String, val name: String, val kcal: Int)
data class ActivityEntry(val id: Long, val day: String, val name: String, val minutes: Int, val kcal: Int)
data class RoutineEntry(val id: Long, val day: String, val name: String, val done: Boolean)
data class DaySnapshot(
    val weights: List<WeightEntry>,
    val foods: List<FoodEntry>,
    val activities: List<ActivityEntry>,
    val routines: List<RoutineEntry>
) {
    val caloriesIn: Int get() = foods.sumOf { it.kcal }
    val manualCaloriesOut: Int get() = activities.sumOf { it.kcal }
}

fun todayKey(): String = LocalDate.now().toString()

/** Local-only personal data. Food catalog updates MUST NOT modify this database. */
class LocalStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 2) {
    companion object { const val DATABASE_NAME = "downsizing_private.db" }
    private val prefs = context.getSharedPreferences("downsizing_settings", Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE weights (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, kg REAL NOT NULL CHECK(kg>0))")
        db.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, kcal INTEGER NOT NULL CHECK(kcal>=0))")
        db.execSQL("CREATE TABLE activities (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, minutes INTEGER NOT NULL CHECK(minutes>0), kcal INTEGER NOT NULL CHECK(kcal>=0))")
        db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, done INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX idx_weights_day ON weights(day)")
        db.execSQL("CREATE INDEX idx_foods_day ON foods(day)")
        db.execSQL("CREATE INDEX idx_activities_day ON activities(day)")
        db.execSQL("CREATE INDEX idx_routines_day ON routines(day)")
        createProfileAndSettings(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createProfileAndSettings(db)
    }

    private fun createProfileAndSettings(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id=1),
            country TEXT NOT NULL, name TEXT NOT NULL, date_of_birth TEXT NOT NULL,
            height_cm REAL NOT NULL CHECK(height_cm>0))""")
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        putSetting(db, "dailyBurnTarget", prefs.getInt("dailyBurnTarget", 0).toString())
        putSetting(db, "goalWeight", prefs.getFloat("goalWeight", 0f).toString())
        putSetting(db, "reminders", prefs.getBoolean("reminders", false).toString())
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // The v2 migration has committed; SQLite is now the source of truth.
        if (!db.isReadOnly) prefs.edit().remove("dailyBurnTarget").remove("goalWeight")
            .remove("reminders").apply()
    }

    fun profile(): UserProfile? = readProfile(readableDatabase)

    private fun readProfile(db: SQLiteDatabase): UserProfile? =
        db.rawQuery("SELECT country,name,date_of_birth,height_cm FROM profile WHERE id=1", null).use { c ->
            if (c.moveToFirst()) UserProfile(c.getString(0), c.getString(1),
                LocalDate.parse(c.getString(2)), c.getDouble(3)) else null
        }

    /** One transaction: a completed profile, starting weight, and goal, or no change at all. */
    fun completeSetup(details: SetupDetails) {
        require(details.isValid())
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (readProfile(db) == null) {
                db.insertOrThrow("profile", null, ContentValues().apply {
                    put("id", 1); put("country", details.profile.country)
                    put("name", details.profile.name.trim())
                    put("date_of_birth", details.profile.dateOfBirth.toString())
                    put("height_cm", details.profile.heightCm)
                })
                db.insertOrThrow("weights", null, ContentValues().apply {
                    put("day", todayKey()); put("kg", details.weightKg)
                })
                putSetting(db, "goalWeight", details.goalKg.toString())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun setting(key: String, fallback: String): String =
        readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else fallback
        }

    private fun putSetting(db: SQLiteDatabase, key: String, value: String) {
        db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES (?,?)", arrayOf(key, value))
    }

    fun addWeight(kg: Double) {
        require(kg in 10.0..600.0)
        writableDatabase.insertOrThrow("weights", null, ContentValues().apply {
            put("day", todayKey()); put("kg", kg)
        })
    }

    fun addFood(name: String, kcal: Int) {
        require(name.isNotBlank() && kcal in 0..10000)
        writableDatabase.insertOrThrow("foods", null, ContentValues().apply {
            put("day", todayKey()); put("name", name.trim().take(120)); put("kcal", kcal)
        })
    }

    fun addActivity(name: String, minutes: Int, kcal: Int) {
        require(name.isNotBlank() && minutes in 1..1440 && kcal in 0..20000)
        writableDatabase.insertOrThrow("activities", null, ContentValues().apply {
            put("day", todayKey()); put("name", name.trim().take(120))
            put("minutes", minutes); put("kcal", kcal)
        })
    }

    fun addRoutine(name: String) {
        require(name.isNotBlank())
        writableDatabase.insertOrThrow("routines", null, ContentValues().apply {
            put("day", todayKey()); put("name", name.trim().take(120)); put("done", 0)
        })
    }

    fun setRoutineDone(id: Long, done: Boolean) {
        writableDatabase.update("routines", ContentValues().apply { put("done", if (done) 1 else 0) },
            "id=?", arrayOf(id.toString()))
    }

    fun deleteEntry(table: String, id: Long) {
        require(table in setOf("weights", "foods", "activities", "routines"))
        writableDatabase.delete(table, "id=?", arrayOf(id.toString()))
    }

    /** User-entered estimated daily baseline expenditure excluding additionally logged workouts. 0 = unset. */
    var dailyBurnTarget: Int
        get() = setting("dailyBurnTarget", "0").toInt()
        set(value) { require(value in 0..10000); putSetting(writableDatabase, "dailyBurnTarget", value.toString()) }

    var goalWeight: Float
        get() = setting("goalWeight", "0").toFloat()
        set(value) { require(value == 0f || value in 10f..600f); putSetting(writableDatabase, "goalWeight", value.toString()) }

    var remindersEnabled: Boolean
        get() = setting("reminders", "false").toBooleanStrict()
        set(value) { putSetting(writableDatabase, "reminders", value.toString()) }

    fun snapshot(): DaySnapshot {
        val weights = mutableListOf<WeightEntry>()
        readableDatabase.rawQuery("SELECT id,day,kg FROM weights ORDER BY day DESC,id DESC LIMIT 180", null).use { c ->
            while (c.moveToNext()) weights += WeightEntry(c.getLong(0), c.getString(1), c.getDouble(2))
        }
        val foods = mutableListOf<FoodEntry>()
        readableDatabase.rawQuery("SELECT id,day,name,kcal FROM foods WHERE day=? ORDER BY id DESC", arrayOf(todayKey())).use { c ->
            while (c.moveToNext()) foods += FoodEntry(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3))
        }
        val activities = mutableListOf<ActivityEntry>()
        readableDatabase.rawQuery("SELECT id,day,name,minutes,kcal FROM activities WHERE day=? ORDER BY id DESC", arrayOf(todayKey())).use { c ->
            while (c.moveToNext()) activities += ActivityEntry(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4))
        }
        val routines = mutableListOf<RoutineEntry>()
        readableDatabase.rawQuery("SELECT id,day,name,done FROM routines WHERE day=? ORDER BY id DESC", arrayOf(todayKey())).use { c ->
            while (c.moveToNext()) routines += RoutineEntry(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) != 0)
        }
        return DaySnapshot(weights, foods, activities, routines)
    }
}
