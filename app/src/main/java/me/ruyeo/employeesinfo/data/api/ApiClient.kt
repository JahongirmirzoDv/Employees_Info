package me.ruyeo.employeesinfo.data.api

import android.content.Context
import com.miguelcatalan.materialsearchview.BuildConfig
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoInterceptor
import me.ruyeo.employeesinfo.data.model.TokenManager
import me.ruyeo.ui.App

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://164.92.180.101:8000/api/v1/"
    private val client = buildClient()

    private val retrofit = buildRetrofit(client)

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private fun buildClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
//        val chucker= ChuckerInterceptor.Builder(App.instance).build()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val builder = OkHttpClient.Builder()
            .addInterceptor(PlutoInterceptor())
            .callTimeout(1, TimeUnit.MINUTES)
            .addNetworkInterceptor(Interceptor { chain ->
                var request = chain.request()
                val builder = request.newBuilder()
                builder.addHeader("Accept", "application/json")
                request = builder.build()
                chain.proceed(request)
            })
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(interceptor)

        }
        return builder.build()
    }

    @JvmStatic
    fun <T> createService(service: Class<T>?): T {
        return retrofit.create(service)
    }

    fun <T> createServiceWithAuth(service: Class<T>?, context: Context): T {
//        val pref =
//            TokenManager.getInstance(context.getSharedPreferences("prefs", Context.MODE_PRIVATE))!!
        val newClient =
            client.newBuilder()
                .addInterceptor(
//                Interceptor { chain ->
//                var request = chain.request()
//                val builder = request.newBuilder()
//                builder.addHeader("Authorization", "token " + pref.token)
//                request = builder.build()
//                chain.proceed(request)
//            }
                    /**
                    Eski interceptor o`rniga yangi [AuthInterceptor] qo`shildi.
                     Sabab: Backend endi JWT token bilan ishlaydigan bo`ldi
                     */
                    AuthInterceptor(context)
                )
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        /*.authenticator(CustomAuthenticator.getInstance(tokenManager)).build()*/
        val newRetrofit = retrofit.newBuilder().client(newClient).build()
        return newRetrofit.create(service)
    }

    class AuthInterceptor(context: Context) : Interceptor {
        private val pref =
            TokenManager.getInstance(context.getSharedPreferences("prefs", Context.MODE_PRIVATE))!!

        override fun intercept(chain: Interceptor.Chain): Response {
            val requestBuilder = chain.request().newBuilder()

            if (!chain.request().url.toString().contains("login")) {
                requestBuilder.addHeader("Authorization", "Bearer ${pref.token}")
            }

            return chain.proceed(requestBuilder.build())
        }
    }
}