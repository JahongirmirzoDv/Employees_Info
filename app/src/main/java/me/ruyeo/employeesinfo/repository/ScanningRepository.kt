package me.ruyeo.employeesinfo.repository

import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.data.local.dao.FlowDao
import me.ruyeo.employeesinfo.data.model.FlowModel
import retrofit2.http.Body

/**
 *Created by farrukh_kh on 6/12/21 6:18 PM
 *me.ruyeo.employeesinfo.repository
 **/
class ScanningRepository(private val apiHelper: ApiHelper, private val appDatabase: AppDatabase,private val flowDao: FlowDao) {

    suspend fun sendFlow(map: HashMap<String,Any>?) = apiHelper.sendFlow(map)
    suspend fun sendFlowWent(id: Int,map: HashMap<String,Any>?) = apiHelper.sendFlowWent(id,map)
    fun getFlow() = appDatabase.getFlowDao().getFlow()
    fun insertFlow(flowModel: FlowModel) = appDatabase.getFlowDao().insertFlow(flowModel)
    fun deleteFlow() = appDatabase.getFlowDao().deleteFlow()

//    fun getFlowWent() = appDatabase.getFlowDao().getFlowWent()
//    fun insertFlowWent(id: Int,flowModel: FlowModel) = appDatabase.getFlowDao().insertFlowWent(id,flowModel)
//    fun deleteFlowWent() = appDatabase.getFlowDao().deleteFlowWent()
}