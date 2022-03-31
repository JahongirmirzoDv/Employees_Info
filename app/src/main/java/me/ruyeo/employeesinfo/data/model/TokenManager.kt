package me.ruyeo.employeesinfo.data.model
import android.content.SharedPreferences

class TokenManager(private val prefs: SharedPreferences) {
    private val editor: SharedPreferences.Editor = prefs.edit()
    fun saveToken(token: String?) {
        editor.putString("ACCESS_TOKEN", token).commit()
    }

    fun saveRefreshToken(token: String?) {
        editor.putString("REFRESH_TOKEN", token).commit()
    }

    fun changeLang(lang: String) {
        editor.putString("lang", lang).commit()
    }

    fun deleteToken() {
        editor.putString("ACCESS_TOKEN", "").commit()
        editor.putString("REFRESH_TOKEN", "").commit()
    }

    val token: String
        get() = prefs.getString("ACCESS_TOKEN", "")!!

    val refreshToken: String
        get() = prefs.getString("REFRESH_TOKEN", "")!!

    val lang: String get() = prefs.getString("lang", "")!!

    companion object {
        private var INSTANCE: TokenManager? = null

        @Synchronized
        fun getInstance(prefs: SharedPreferences): TokenManager? {
            if (INSTANCE == null) {
                INSTANCE = TokenManager(prefs)
            }
            return INSTANCE
        }
    }

}