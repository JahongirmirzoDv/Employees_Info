package me.ruyeo.employeesinfo.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Errors {
    @SerializedName("field")
    @Expose
    var field: String? = null

    @SerializedName("message")
    @Expose
    var message: String? = null
}