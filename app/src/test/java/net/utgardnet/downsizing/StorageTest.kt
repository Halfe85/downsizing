package net.utgardnet.downsizing

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import net.utgardnet.downsizing.data.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val details = SetupDetails(UserProfile("NO", "Kari", LocalDate.of(1990, 6, 24), 172.5), 82.5, 75.0)

    @Before fun reset() {
        context.deleteDatabase(LocalStore.DATABASE_NAME)
        context.deleteDatabase(FoodCatalog.DATABASE_NAME)
        context.getSharedPreferences("downsizing_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() = reset()

    @Test fun completedSetupSurvivesReopenAndDoesNotDuplicateStartingWeight() {
        LocalStore(context).use { store ->
            assertNull(store.profile())
            store.completeSetup(details)
            store.completeSetup(details.copy(weightKg = 90.0))
        }
        LocalStore(context).use { store ->
            assertEquals(details.profile, store.profile())
            assertEquals(75f, store.goalWeight)
            assertEquals(listOf(82.5), store.snapshot().weights.map { it.kg })
        }
    }

    @Test fun invalidProfileWritesNothing() {
        LocalStore(context).use { store ->
            assertThrows(IllegalArgumentException::class.java) {
                store.completeSetup(details.copy(profile = details.profile.copy(country = "SE")))
            }
            assertNull(store.profile())
            assertTrue(store.snapshot().weights.isEmpty())
            assertEquals(0f, store.goalWeight)
        }
    }

    @Test fun failedGoalSaveRollsBackProfileAndWeight() {
        LocalStore(context).use { store ->
            store.writableDatabase.execSQL("""CREATE TRIGGER fail_goal BEFORE INSERT ON settings
                WHEN NEW.key='goalWeight' BEGIN SELECT RAISE(ABORT, 'simulated failure'); END""")
            assertThrows(android.database.SQLException::class.java) { store.completeSetup(details) }
            assertNull(store.profile())
            assertTrue(store.snapshot().weights.isEmpty())
            assertEquals(0f, store.goalWeight)
            store.writableDatabase.execSQL("DROP TRIGGER fail_goal")
            store.completeSetup(details)
            assertEquals(details.profile, store.profile())
        }
    }

    @Test fun catalogReplacementCannotRemovePrivateRecords() {
        LocalStore(context).use { store ->
            store.completeSetup(details)
            store.addFood("Private breakfast", 400)
            FoodCatalog(context).use { catalog ->
                val db = catalog.writableDatabase
                assertNotEquals(store.readableDatabase.path, db.path)
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    val names = mutableListOf<String>()
                    while (c.moveToNext()) names += c.getString(0)
                    assertFalse(names.contains("profile"))
                    assertFalse(names.contains("foods"))
                }
                db.rawQuery("SELECT COUNT(*) FROM food_catalog", null).use { c ->
                    c.moveToFirst(); assertEquals(0, c.getInt(0))
                }
            }
            assertTrue(context.deleteDatabase(FoodCatalog.DATABASE_NAME))
            FoodCatalog(context).use { it.writableDatabase }
            assertEquals(details.profile, store.profile())
            assertEquals("Private breakfast", store.snapshot().foods.single().name)
            assertEquals(82.5, store.snapshot().weights.single().kg, 0.0)
        }
    }

    @Test fun versionOneMigrationPreservesAllRecordsAndImportsSettings() {
        val path = context.getDatabasePath(LocalStore.DATABASE_NAME)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE weights (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, kg REAL NOT NULL CHECK(kg>0))")
            db.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, kcal INTEGER NOT NULL CHECK(kcal>=0))")
            db.execSQL("CREATE TABLE activities (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, minutes INTEGER NOT NULL CHECK(minutes>0), kcal INTEGER NOT NULL CHECK(kcal>=0))")
            db.execSQL("CREATE TABLE routines (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, name TEXT NOT NULL, done INTEGER NOT NULL)")
            db.execSQL("INSERT INTO weights VALUES (1,?,88.0)", arrayOf(todayKey()))
            db.execSQL("INSERT INTO foods VALUES (1,?,'Breakfast',300)", arrayOf(todayKey()))
            db.execSQL("INSERT INTO activities VALUES (1,?,'Walk',30,100)", arrayOf(todayKey()))
            db.execSQL("INSERT INTO routines VALUES (1,?,'Water',1)", arrayOf(todayKey()))
            db.version = 1
        }
        val prefs = context.getSharedPreferences("downsizing_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("goalWeight", 77f).putInt("dailyBurnTarget", 2100).putBoolean("reminders", true).commit()
        LocalStore(context).use { store ->
            assertNull(store.profile())
            assertEquals(2, store.readableDatabase.version)
            assertEquals(88.0, store.snapshot().weights.single().kg, 0.0)
            assertEquals("Breakfast", store.snapshot().foods.single().name)
            assertEquals("Walk", store.snapshot().activities.single().name)
            assertTrue(store.snapshot().routines.single().done)
            assertEquals(77f, store.goalWeight)
            assertEquals(2100, store.dailyBurnTarget)
            assertTrue(store.remindersEnabled)
            store.completeSetup(details)
            assertEquals(2, store.snapshot().weights.size)
            assertEquals(1, store.snapshot().foods.size)
        }
        LocalStore(context).use { store ->
            assertEquals(75f, store.goalWeight)
            assertEquals(2100, store.dailyBurnTarget)
        }
        assertFalse(prefs.contains("goalWeight"))
    }
}
