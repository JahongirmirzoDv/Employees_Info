package me.ruyeo.employeesinfo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 *Created by farrukh_kh on 6/11/21 5:21 PM
 *me.ruyeo.employeesinfo.data.model
 **/

/**
 Backenddan keladigan xodimlar uchun data class
 Xodimlar ro`yxati local database da saqlanadi
 */
@Entity(tableName = "staff_from_api")
data class Staff(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "first_name")
    @SerializedName("first_name")
    val firstName: String,
    @ColumnInfo(name = "last_name")
    @SerializedName("second_name")
    val lastName: String,
    @ColumnInfo(name = "company")
    @SerializedName("company")
    val company: String,
    @ColumnInfo(name = "position")
    @SerializedName("position")
    val position: String,
    @ColumnInfo(name = "image")
    val image: String,
    @ColumnInfo(name = "qr_code")
    @SerializedName("qr_code")
    val qrCode: String,
)
