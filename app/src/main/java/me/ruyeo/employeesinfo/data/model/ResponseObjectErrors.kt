package me.ruyeo.employeesinfo.data.model
class ResponseObjectErrors<T>(
        var status: Boolean,
        var message: String,
        var data: T?,
        var errors: List<Error>
)

class Error(
        var field: String,
        var message: String
)

