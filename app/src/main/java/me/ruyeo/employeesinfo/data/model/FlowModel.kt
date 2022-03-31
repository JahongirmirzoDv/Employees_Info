package me.ruyeo.employeesinfo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 *Created by farrukh_kh on 6/12/21 6:21 PM
 *me.ruyeo.employeesinfo.data.model
 **/

/**
 Xodim keldi ketdisi (api/v1/flow endpoint) uchun data class
 */

@Entity(tableName = "flow_table")
data class FlowModel(
    @PrimaryKey
    @ColumnInfo(name = "staff")
    @SerializedName("staff")
    var staffId: Int,
    @ColumnInfo(name = "came")
    @SerializedName("came")
    val cameTime: String?,
    @ColumnInfo(name = "came_lunch")
    @SerializedName("came_lunch")
    val came_lunch: String?,
    @ColumnInfo(name = "went_lunch")
    @SerializedName("went_lunch")
    val went_lunch: String?,
    @ColumnInfo(name = "went")
    @SerializedName("went")
    val wentTime: String?
)
