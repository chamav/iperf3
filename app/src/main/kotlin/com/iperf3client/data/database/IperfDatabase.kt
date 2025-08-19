package com.iperf3client.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.iperf3client.data.database.dao.ServerDao
import com.iperf3client.data.database.dao.TestResultDao
import com.iperf3client.data.database.entity.ServerEntity
import com.iperf3client.data.database.entity.TestResultEntity

@Database(
    entities = [
        ServerEntity::class,
        TestResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IperfDatabase : RoomDatabase() {
    
    abstract fun serverDao(): ServerDao
    abstract fun testResultDao(): TestResultDao
    
    companion object {
        const val DATABASE_NAME = "iperf_database"
        
        @Volatile
        private var INSTANCE: IperfDatabase? = null
        
        fun getDatabase(context: Context): IperfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IperfDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}