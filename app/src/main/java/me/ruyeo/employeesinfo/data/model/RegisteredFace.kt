package me.ruyeo.employeesinfo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *Created by farrukh_kh on 6/11/21 10:40 AM
 *kh.farrukh.facerecognition.database
 **/

/**
 FaceID tanib olgan xodimlar uchun data class
 Tanilgan yuzlar local database ga saqlanadi
 */
@Entity(tableName = "registered_faces")
data class RegisteredFace(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "bio_data")
    val bioData: Any
) {
    fun toRecognition() = Recognition(id, name, -1f, null, null, bioData, null)
}