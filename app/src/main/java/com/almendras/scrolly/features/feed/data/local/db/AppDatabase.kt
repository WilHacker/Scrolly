package com.almendras.scrolly.features.feed.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "scrolly_database"
            )
                // La BD solo cachea favoritos: ante un cambio de esquema (o una
                // versión vieja instalada en el dispositivo) se recrea en vez de crashear
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
