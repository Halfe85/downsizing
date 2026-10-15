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
class LocalStore(context: Context) : SQLiteOpenHelper(context, "downsizing_private.db", null, 1) {
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
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations must preserve personal records. NEVER drop tables.
        throw IllegalStateException("Database migration is required")
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
        get() = prefs.getInt("dailyBurnTarget", 0)
        set(value) { require(value in 0..10000); prefs.edit().putInt("dailyBurnTarget", value).apply() }

    var goalWeight: Float
        get() = prefs.getFloat("goalWeight", 0f)
        set(value) { require(value == 0f || value in 10f..600f); prefs.edit().putFloat("goalWeight", value).apply() }

    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders", false)
        set(value) { prefs.edit().putBoolean("reminders", value).apply() }

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
