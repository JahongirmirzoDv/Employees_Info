package me.ruyeo.employeesinfo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.ruyeo.employeesinfo.data.model.Staff

/**
 *Created by farrukh_kh on 6/11/21 5:38 PM
 *me.ruyeo.employeesinfo.data.local.dao
 **/

/**
 Backend dan keladigan xodimlar ro`yxatini saqlab qo`yish uchun
 */
@Dao
interface StaffDao {
    @Query("SELECT * FROM STAFF_FROM_API WHERE id=:id")
    fun getStaffById(id: Int) : Staff

    @Query("SELECT * FROM STAFF_FROM_API")
    fun getAll() : List<Staff>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(staffList: List<Staff>)

    @Query("DELETE FROM STAFF_FROM_API")
    suspend fun clearAll()
}