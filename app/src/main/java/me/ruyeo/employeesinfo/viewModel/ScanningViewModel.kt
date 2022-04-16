package me.ruyeo.employeesinfo.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.ruyeo.employeesinfo.data.model.FlowModel
import me.ruyeo.employeesinfo.data.model.ResponseObject
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.repository.ScanningRepository
import org.joda.time.DateTime
import retrofit2.HttpException
import retrofit2.http.Body
import java.io.IOException

/**
 *Created by farrukh_kh on 6/12/21 6:30 PM
 *me.ruyeo.employeesinfo.viewModel
 **/
class ScanningViewModel(private val repository: ScanningRepository) : ViewModel() {
    private var _flowState = MutableStateFlow<FlowUiState>(FlowUiState.EMPTY)
    val flowState: StateFlow<FlowUiState> = _flowState

    private var _flowStateWent = MutableStateFlow<FlowWentUiState>(FlowWentUiState.EMPTY)
    val flowStateWent: StateFlow<FlowWentUiState> = _flowStateWent

    fun sendFlow(map: HashMap<String,Any>?) = viewModelScope.launch {
        _flowState.value = FlowUiState.LOADING
        try {
            val response = repository.sendFlow(map)
            _flowState.value = FlowUiState.SUCCESS(response)
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> FlowUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        _flowState.value = FlowUiState.ERROR("Server bilan muommo")
                    } else {
                        _flowState.value = FlowUiState.ERROR(throwable.message())
                    }
                }
                is Exception -> {
                    _flowState.value = FlowUiState.ERROR(throwable.message.toString())
                }
            }
        }
    }
    fun sendFlowAction(map: HashMap<String,Any>?) = viewModelScope.launch {
        _flowState.value = FlowUiState.LOADING
        try {
            val response = repository.sendFlowAction(map)
            _flowState.value = FlowUiState.SUCCESS(response)
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> FlowUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        _flowState.value = FlowUiState.ERROR("Server bilan muommo")
                    } else {
                        _flowState.value = FlowUiState.ERROR(throwable.message())
                    }
                }
                is Exception -> {
                    _flowState.value = FlowUiState.ERROR(throwable.message.toString())
                }
            }
        }
    }

    fun sendFlowWent(id: Int,map: HashMap<String,Any>?) = viewModelScope.launch {
        _flowStateWent.value = FlowWentUiState.LOADING
        try {
            repository.sendFlowWent(id,map)
            _flowStateWent.value = FlowWentUiState.SUCCESS
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> FlowWentUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        Log.e("error", throwable.message())
                        _flowStateWent.value = FlowWentUiState.ERROR("Server bilan muommo")
                    } else {
                        _flowStateWent.value = FlowWentUiState.ERROR(throwable.message())
                        Log.d("errrf",throwable.message())
                    }
                }
                is Exception -> {
                    _flowStateWent.value = FlowWentUiState.ERROR(throwable.message.toString())
                    Log.d("errrfs",throwable.message.toString())
                }
            }
        }
    }

    fun sendFlowWentAction(id: Int,map: HashMap<String,Any>?) = viewModelScope.launch {
        _flowStateWent.value = FlowWentUiState.LOADING
        try {
            repository.sendFlowWent(id,map)
            _flowStateWent.value = FlowWentUiState.SUCCESS
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> FlowWentUiState.ERROR("Internet bilan bog'liq muommo")
                is HttpException -> {
                    if (throwable.code() >= 500) {
                        Log.e("error", throwable.message())
                        _flowStateWent.value = FlowWentUiState.ERROR("Server bilan muommo")
                    } else {
                        _flowStateWent.value = FlowWentUiState.ERROR(throwable.message())
                        Log.d("errrf",throwable.message())
                    }
                }
                is Exception -> {
                    _flowStateWent.value = FlowWentUiState.ERROR(throwable.message.toString())
                    Log.d("errrfs",throwable.message.toString())
                }
            }
        }
    }

    fun getFlow() = repository.getFlow()
    fun insertFlow(flowModel: FlowModel) = repository.insertFlow(flowModel)
    fun deleteFlow() = repository.deleteFlow()

//    fun getFlowWent() = repository.getFlowWent()
//    fun insertFlowWent(id: Int,flowModel: FlowModel) = repository.insertFlowWent(id,flowModel)
//    fun deleteFlowWent() = repository.deleteFlowWent()

    sealed class FlowUiState {
        data class SUCCESS(val response: ResponseObject) : FlowUiState()
        data class ERROR(val message: String) : FlowUiState()
        object LOADING : FlowUiState()
        object EMPTY : FlowUiState()
    }

    sealed class FlowWentUiState {
        object SUCCESS : FlowWentUiState()
        data class ERROR(val message: String) : FlowWentUiState()
        object LOADING : FlowWentUiState()
        object EMPTY : FlowWentUiState()
    }
}