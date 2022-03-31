package me.ruyeo.employeesinfo.data.api

import me.ruyeo.employeesinfo.data.model.ResponseObject
import me.ruyeo.employeesinfo.data.model.Staff
import retrofit2.http.*

@JvmSuppressWildcards
interface ApiService {

    /**
     Token olish uchun. Eski login endpoint o`rniga ishlatiladi
     */
    @POST("token/obtain")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): ResponseObject

    @GET("logout")
    suspend fun logout(): String

    /**
     Barcha xodimlar ro`yxatini olish uchun
     */
    @GET("staff")
    suspend fun getAllStaff(): List<Staff>

    /**
    Xodim keldi ketdisini post qilish uchun
     */
    @POST("flow/")
    suspend fun sendFlow(@Body map: HashMap<String,Any>?): ResponseObject

    @PUT("flow/{id}/")
    suspend fun sendFlowWent(@Path("id") id: Int, @Body map: HashMap<String,Any>?)

    //    @POST("login")
//    @FormUrlEncoded
//    suspend fun login(@Field("username") username: String, @Field("password") password: String) : ResponseObject

//    @DELETE("api/userprofile/{id}/")
//    suspend fun deleteUser(@Path("id") id: Int): ResponseObject
//
//    @POST("api/userprofile/")
//    suspend fun registration(@Body map: HashMap<String, Any>?): ResponseObject

    /*  @GET("api/cart")
      suspend fun getCart(): List<Cart>

      @GET("api/debtor")
      suspend fun getDebtor(): List<Debtor>
      */
}