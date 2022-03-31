package me.ruyeo.employeesinfo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.ruyeo.employeesinfo.data.local.dao.FacesDao
import me.ruyeo.employeesinfo.data.local.dao.FlowDao
import me.ruyeo.employeesinfo.data.local.dao.StaffDao
import me.ruyeo.employeesinfo.data.model.FlowModel
import me.ruyeo.employeesinfo.data.model.RegisteredFace
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.faceDetect.database.CustomTypeConverters

/**
 *Created by farrukh_kh on 6/9/21 3:32 PM
 *kh.farrukh.facerecognition.database
 **/
@Database(entities = [RegisteredFace::class, Staff::class,FlowModel::class], version = 3, exportSchema = false)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getFacesDao(): FacesDao
    abstract fun getStaffDao(): StaffDao
    abstract fun getFlowDao(): FlowDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_database"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}