package me.ruyeo.employeesinfo.data.model
class ResponseListErrors<T>(
    var status: String,
    var message: String,
    var current_page: Int,
    var data: List<T>,
    var errors: List<Errors>,
    var next_page_url: String,
    val total: Int
)

