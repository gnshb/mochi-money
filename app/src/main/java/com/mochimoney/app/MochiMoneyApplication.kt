package com.mochimoney.app

import android.app.Application
import com.mochimoney.app.data.AppContainer

class MochiMoneyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
