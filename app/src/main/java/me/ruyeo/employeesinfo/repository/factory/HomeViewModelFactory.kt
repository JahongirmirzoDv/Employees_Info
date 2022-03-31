package me.ruyeo.employeesinfo.repository.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.local.dao.StaffDao
import me.ruyeo.employeesinfo.repository.HomeRepository
import me.ruyeo.employeesinfo.viewModel.HomeViewModel

class HomeViewModelFactory(private val apiHelper: ApiHelper, private val dao: StaffDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(HomeRepository(apiHelper,dao)) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}