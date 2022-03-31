package me.ruyeo.employeesinfo.data.model

class ResponseObjects<T>(
    var debtor: T,
    var message: String,
    var token: String
)
