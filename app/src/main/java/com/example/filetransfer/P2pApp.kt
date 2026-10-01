package com.example.filetransfer

import android.app.Application
import com.example.filetransfer.di.AppContainer

class P2pApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        appContainer = AppContainer(this)
    }
}
