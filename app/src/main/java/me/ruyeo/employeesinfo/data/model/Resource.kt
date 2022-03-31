package me.ruyeo.employeesinfo.data.model
import me.ruyeo.employeesinfo.utils.CONSTANTS.ERROR
import me.ruyeo.employeesinfo.utils.CONSTANTS.LOADING
import me.ruyeo.employeesinfo.utils.CONSTANTS.SUCCESS


data class Resource<out T> (val status: String, val data: T?, val message: String?) {
    companion object{
        fun <T> success (data: T): Resource<T> = Resource(status = SUCCESS, data = data, message = null)
        fun <T> error (data: T, message:String?): Resource<T> = Resource(status = ERROR, data = data, message = message)
        fun <T> loading (data: T): Resource<T> = Resource(status = LOADING, data = data, message = null)
    }
}