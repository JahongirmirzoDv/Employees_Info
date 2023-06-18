package me.ruyeo.ui

import android.app.Application
import android.content.Context
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoNetworkPlugin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Pluto.Installer(this)
            .addPlugin(PlutoNetworkPlugin("network"))
            .install()
    }

    companion object {
        lateinit var instance: App private set
    }
}