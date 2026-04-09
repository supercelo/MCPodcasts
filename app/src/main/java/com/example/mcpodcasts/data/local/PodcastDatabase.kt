package com.example.mcpodcasts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: PodcastDatabase? = null

        fun getInstance(context: Context): PodcastDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    PodcastDatabase::class.java,
                    "mcpodcasts.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { database ->
                    INSTANCE = database
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE podcasts
                    ADD COLUMN notifyNewEpisodes INTEGER NOT NULL DEFAULT 1
                    """
                )
                db.execSQL(
                    """
                    ALTER TABLE podcasts
                    ADD COLUMN introSkipSeconds INTEGER NOT NULL DEFAULT 0
                    """
                )
                db.execSQL(
                    """
                    ALTER TABLE podcasts
                    ADD COLUMN outroSkipSeconds INTEGER NOT NULL DEFAULT 0
                    """
                )
                db.execSQL(
                    """
                    ALTER TABLE episodes
                    ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0
                    """
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE podcasts
                    ADD COLUMN includeInQueue INTEGER NOT NULL DEFAULT 1
                    """
                )
            }
        }
    }
}
