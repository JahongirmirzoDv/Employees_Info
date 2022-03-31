package me.ruyeo.employeesinfo.repository.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.repository.LoginRepository
import me.ruyeo.employeesinfo.viewModel.LoginViewModel

class LoginViewModelFactory(
    private val apiHelper: ApiHelper,
    private val appDatabase: AppDatabase,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(LoginRepository(apiHelper, appDatabase, context)) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}