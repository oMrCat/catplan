package io.github.catplan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CatEntity::class, WeightEntryEntity::class, CareTaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CatPlanDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao
    abstract fun weightDao(): WeightDao
    abstract fun careTaskDao(): CareTaskDao

    companion object {
        @Volatile
        private var instance: CatPlanDatabase? = null

        fun get(context: Context): CatPlanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatPlanDatabase::class.java,
                    "catplan.db",
                ).build().also { instance = it }
            }
    }
}
