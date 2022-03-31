package me.ruyeo.employeesinfo.repository

import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.local.dao.StaffDao

class HomeRepository(private val apiHelper: ApiHelper,private val dao: StaffDao) {
    suspend fun logout() = apiHelper.logout()

    fun getAllStaff() = dao.getAll()
}