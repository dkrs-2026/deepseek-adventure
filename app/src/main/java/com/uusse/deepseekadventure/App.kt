package com.uusse.deepseekadventure

import android.app.Application
import com.uusse.deepseekadventure.data.AppContainer

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
