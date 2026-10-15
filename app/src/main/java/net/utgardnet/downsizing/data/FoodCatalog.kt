package net.utgardnet.downsizing.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Reference food data only. No profiles, meal history, health records or advertising identifiers. */
class FoodCatalog(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    companion object { const val DATABASE_NAME = "downsizing_food.db" }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE food_catalog (
            id TEXT PRIMARY KEY, country TEXT NOT NULL, name TEXT NOT NULL,
            barcode TEXT, kcal_per_100g REAL NOT NULL CHECK(kcal_per_100g>=0),
            source TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX idx_catalog_country_name ON food_catalog(country,name)")
        db.execSQL("CREATE INDEX idx_catalog_barcode ON food_catalog(barcode)")
        // Intentionally empty until a licensed Norwegian catalog is supplied.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Food catalog migration required")
    }
}
