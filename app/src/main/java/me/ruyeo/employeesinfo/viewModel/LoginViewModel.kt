package me.ruyeo.employeesinfo.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.ruyeo.employeesinfo.data.model.ResponseObject
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.repository.LoginRepository
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(private var repository: LoginRepository) : ViewModel() {
    private var _loginState = MutableStateFlow<LoginUiState>(LoginUiState.EMPTY)
    val loginState: StateFlow<LoginUiState> = _loginState
    private var _staffState = MutableStateFlow<StaffListState>(StaffListState.EMPTY)
    val staffState: StateFlow<StaffListState> = _staffState

    fun login(username: String, password: String) = viewModelScope.launch {
        _loginState.value = LoginUiState.LOADING
        try {
            val response = repository.login(username, password)
            _loginState.value = LoginUiState.SUCCESS(response)
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> LoginUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        _loginState.value = LoginUiState.ERROR("Server bilan muommo")
                    } else {
                        _loginState.value = LoginUiState.ERROR(throwable.message())
                    }
                }
            }
        }
    }

    fun getAllStaff() = viewModelScope.launch {
        _staffState.value = StaffListState.LOADING
        try {
            val response = repository.getAllStaff()
            repository.saveAllStaff(response)
            _staffState.value = StaffListState.SUCCESS(response)
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> StaffListState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        _staffState.value = StaffListState.ERROR("Server bilan muommo")
                    } else {
                        _staffState.value = StaffListState.ERROR(throwable.message())
                    }
                }
            }
        }
    }

    fun clearAll() = viewModelScope.launch {
        repository.clearAll()
    }

    sealed class LoginUiState {
        data class SUCCESS(val user: ResponseObject) : LoginUiState()
        data class ERROR(val message: String) : LoginUiState()
        object LOADING : LoginUiState()
        object EMPTY : LoginUiState()
    }

    sealed class StaffListState {
        data class SUCCESS(val staffList: List<Staff>) : StaffListState()
        data class ERROR(val message: String) : StaffListState()
        object LOADING : StaffListState()
        object EMPTY : StaffListState()
    }
}