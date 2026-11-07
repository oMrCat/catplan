package io.github.omrcat.catplan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CatEntity::class, WeightEntryEntity::class, CareTaskEntity::class, ExpenseEntity::class, PhotoEntity::class, DailyEntity::class, HealthNoteEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class CatPlanDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao
    abstract fun weightDao(): WeightDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun photoDao(): PhotoDao
    abstract fun dailyDao(): DailyDao
    abstract fun healthNoteDao(): HealthNoteDao

    companion object {
        @Volatile
        private var instance: CatPlanDatabase? = null

        /** v1 → v2：新增 expenses 表（多猫记账，v0.2） */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        catId INTEGER,
                        amountCents INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT,
                        dateEpochDay INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(catId) REFERENCES cats(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_catId ON expenses(catId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_dateEpochDay ON expenses(dateEpochDay)")
            }
        }

        /** v2 → v3：新增 photos 表（成长相册，v0.2） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        catId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        note TEXT,
                        dateEpochDay INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(catId) REFERENCES cats(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_catId ON photos(catId)")
            }
        }

        /** v3 → v4：新增 dailies 表（喂食/铲屎打卡，v0.3） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dailies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        catId INTEGER,
                        dateEpochDay INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(catId) REFERENCES cats(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dailies_catId ON dailies(catId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dailies_dateEpochDay ON dailies(dateEpochDay)")
            }
        }

        /** v4 → v5：新增 health_notes 表（健康日记，v0.3） */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        catId INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        appetite TEXT,
                        energy TEXT,
                        poop TEXT,
                        urine TEXT,
                        text TEXT,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(catId) REFERENCES cats(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_health_notes_catId ON health_notes(catId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_health_notes_dateEpochDay ON health_notes(dateEpochDay)")
            }
        }

        fun get(context: Context): CatPlanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatPlanDatabase::class.java,
                    "catplan.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
