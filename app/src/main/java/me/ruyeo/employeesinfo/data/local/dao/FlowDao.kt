package me.ruyeo.employeesinfo.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.ruyeo.employeesinfo.data.model.FlowModel
import me.ruyeo.employeesinfo.data.model.RegisteredFace

@Dao
interface FlowDao {

    @Query("SELECT * FROM flow_table")
    fun getFlow(): LiveData<FlowModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFlow(flowModel: FlowModel)

    @Query("DELETE FROM flow_table")
    fun deleteFlow()

//    @Query("SELECT * FROM flow_table")
//    fun getFlowWent(): LiveData<FlowModel>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertFlowWent(id: Int,flowModel: FlowModel)
//
//    @Query("DELETE FROM flow_table")
//    fun deleteFlowWent()
}