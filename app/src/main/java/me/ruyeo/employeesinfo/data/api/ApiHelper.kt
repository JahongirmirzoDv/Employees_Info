package me.ruyeo.employeesinfo.data.api

import me.ruyeo.employeesinfo.data.model.FlowModel
import retrofit2.http.Body


class ApiHelper(private val apiService: ApiService) {
    suspend fun login(username: String, password: String) = apiService.login(username, password)

    suspend fun logout() = apiService.logout()

    suspend fun getAllStaff() = apiService.getAllStaff()

    /**
    Xodim keldi ketdisini yuborish uchun
     [FlowModel] ni ko`rib chiqing
     */
    suspend fun sendFlow(map: HashMap<String,Any>?) =
        apiService.sendFlow(map)

    suspend fun sendFlowWent(id: Int,map: HashMap<String,Any>?) =
        apiService.sendFlowWent(id,map)

//    suspend fun registration(map: HashMap<String, Any>?) = apiService.registration(map)

}