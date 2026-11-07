package io.github.catplan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CatEntity::class, WeightEntryEntity::class, CareTaskEntity::class, ExpenseEntity::class, PhotoEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class CatPlanDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao
    abstract fun weightDao(): WeightDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun photoDao(): PhotoDao

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

        fun get(context: Context): CatPlanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatPlanDatabase::class.java,
                    "catplan.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
