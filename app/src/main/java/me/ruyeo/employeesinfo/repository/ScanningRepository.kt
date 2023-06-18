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
    suspend fun sendFlowAction(map: HashMap<String,Any>?) = apiHelper.sendFlowAction(map)
    suspend fun sendFlowWent(map: HashMap<String,Any>?) = apiHelper.sendFlowWent(map)
    suspend fun sendWent(map: HashMap<String,Any>?) = apiHelper.sendWent(map)
    suspend fun sendCameLaunch(map: HashMap<String,Any>?) = apiHelper.sendCameLaunch(map)
    suspend fun sendWentLaunch(map: HashMap<String,Any>?) = apiHelper.sendWentLaunch(map)
    suspend fun kitchen(map: HashMap<String,Any>?) = apiHelper.kitchen(map)
//    suspend fun sendFlowWentAction(id: Int,map: HashMap<String,Any>?) = apiHelper.sendFlowWentAction(id,map)
    fun getFlow() = appDatabase.getFlowDao().getFlow()
    fun insertFlow(flowModel: FlowModel) = appDatabase.getFlowDao().insertFlow(flowModel)
    fun updateFlow(flowModel: FlowModel) = appDatabase.getFlowDao().updateFlow(flowModel)
    fun deleteFlow() = appDatabase.getFlowDao().deleteFlow()

//    fun getFlowWent() = appDatabase.getFlowDao().getFlowWent()
//    fun insertFlowWent(id: Int,flowModel: FlowModel) = appDatabase.getFlowDao().insertFlowWent(id,flowModel)
//    fun deleteFlowWent() = appDatabase.getFlowDao().deleteFlowWent()
}