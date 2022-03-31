package me.ruyeo.employeesinfo.data.local.dao

import androidx.room.*
import me.ruyeo.employeesinfo.data.model.RegisteredFace

/**
 *Created by farrukh_kh on 6/9/21 3:30 PM
 *kh.farrukh.facerecognition.database
 **/

/**
 Face Id tanigan xodim yuzlarini saqlash uchun.
 Dastur ishga tushganda backenddan xodimlar ro`yxati olinadi
 va har bir xodimni rasmi FaceID orqali tanitilib, yuz ma`lumotlari
 saqlab qo`yiladi
 */
@Dao
interface FacesDao {
    @Query("SELECT * FROM REGISTERED_FACES")
    fun getAll(): List<RegisteredFace>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFace(face: RegisteredFace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFaces(faces: List<RegisteredFace>)

    @Delete
    fun deleteFace(face: RegisteredFace)

    @Query("DELETE FROM registered_faces")
    suspend fun clearAll()
}