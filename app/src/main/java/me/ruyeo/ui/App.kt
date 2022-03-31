package me.ruyeo.ui

import android.app.Application
import android.content.Context
import com.mocklets.pluto.Pluto

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Pluto.initialize(this)
    }

    companion object {
        lateinit var instance: App private set
    }
}