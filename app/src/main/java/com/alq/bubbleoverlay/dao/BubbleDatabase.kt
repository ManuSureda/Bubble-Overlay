package com.alq.bubbleoverlay.dao

import BubbleDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alq.bubbleoverlay.utils.Converters

@Database(
    entities = [Bubble::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BubbleDatabase : RoomDatabase() {
    abstract fun bubbleDao(): BubbleDao // Referencia al DAO

    companion object {
        @Volatile
        private var INSTANCE: BubbleDatabase? = null

        fun getInstance(context: Context): BubbleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BubbleDatabase::class.java,
                    "bubble_database" // Nombre del archivo de la BD
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
