package com.almendras.scrolly

import android.app.Application
import com.almendras.scrolly.core.di.AppContainer

class ScrollyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
