package me.ruyeo.employeesinfo

import android.content.Context
import android.content.SharedPreferences
import me.ruyeo.employeesinfo.data.model.Staff

class SharedPref(context: Context) {

    private var mySharedPref: SharedPreferences =
        context.getSharedPreferences("filename", Context.MODE_PRIVATE)

    fun setUser(staff: Staff) {
        val editor = mySharedPref.edit()
        editor.putInt("id", staff.id)
        editor.putString("firstName", staff.firstName)
        editor.putString("lastName", staff.lastName)
        editor.putString("image", staff.image)
        editor.putString("qrcode", staff.qrCode)
        editor.apply()
    }

   /* fun getUser(): Staff {
        return Staff(
            mySharedPref.getInt("id",0),
            mySharedPref.getString("firstName","")!!,
            mySharedPref.getString("lastName","")!!,
            mySharedPref.getString("image","")!!,
            mySharedPref.getString("qrcode","")!!
        )
    }*/

  /*  fun getUserData(): UserX {
        return UserX(
            mySharedPref.getInt("filial",0),
            mySharedPref.getString("first_name", "")!!,
            mySharedPref.getInt("id", 0),
            mySharedPref.getString("last_name","")!!,
            mySharedPref.getString("password","")!!,
            mySharedPref.getInt("staff",0),
            mySharedPref.getString("username","")!!
        )
    }
    fun setUserData(user: UserX) {
        val editor = mySharedPref.edit()
        editor.putInt("id", user.id)
        editor.putString("username", user.username)
        editor.putString("first_name", user.first_name)
        editor.putString("last_name", user.last_name)
        editor.putInt("staff", user.staff)
        editor.putInt("filial",user.filial)

        editor.apply()
    }*/

    fun setFirstEnter(state: Boolean?) {
        val editor = mySharedPref.edit()
        editor.putBoolean("first_enter", state!!)
        editor.apply()
    }
    fun isFirstEnter(): Boolean {
        return mySharedPref.getBoolean("first_enter", false)
    }

    fun setId(state: Int?) {
        val editor = mySharedPref.edit()
        editor.putInt("id", state!!)
        editor.apply()
    }
    fun getId(): Int {
        return mySharedPref.getInt("id", 0)
    }

}
