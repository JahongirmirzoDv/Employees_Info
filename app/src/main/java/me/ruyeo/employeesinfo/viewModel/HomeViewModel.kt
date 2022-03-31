package me.ruyeo.employeesinfo.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.repository.HomeRepository
import retrofit2.HttpException
import java.io.IOException

class HomeViewModel(private var repository: HomeRepository) : ViewModel() {
    private var _homeState = MutableStateFlow<HomeUiState>(HomeUiState.EMPTY)
    var homeState = _homeState

    private var _staffState = MutableStateFlow<StaffUiState>(StaffUiState.EMPTY)
    var staffState = _staffState

    fun logout() = viewModelScope.launch {
        _homeState.value = HomeUiState.LOADING
        try {
             repository.logout()
            _homeState.value = HomeUiState.SUCCESS
        }
        catch (throwable : Throwable){
            when(throwable){
                is IOException -> HomeUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500){
                        _homeState.value = HomeUiState.ERROR("Server bilan muommo")
                    }else{
                        _homeState.value = HomeUiState.ERROR(throwable.message())
                    }
                }
            }
        }
    }

    fun getAllStaff() = viewModelScope.launch {
        _staffState.value = StaffUiState.LOADING
        try {
            val response = repository.getAllStaff()
            _staffState.value = StaffUiState.SUCCESS(response)
        }catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> StaffUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        _staffState.value = StaffUiState.ERROR("Server bilan muommo")
                    } else {
                        _staffState.value = StaffUiState.ERROR(throwable.message())
                    }
                }
            }
        }
    }

    sealed class HomeUiState {
        object SUCCESS : HomeUiState()
        data class ERROR(val message: String) : HomeUiState()
        object LOADING : HomeUiState()
        object EMPTY : HomeUiState()

    }

    sealed class StaffUiState {
        data class SUCCESS(val data: List<Staff>) : StaffUiState()
        data class ERROR(val message: String) : StaffUiState()
        object LOADING : StaffUiState()
        object EMPTY : StaffUiState()

    }
}