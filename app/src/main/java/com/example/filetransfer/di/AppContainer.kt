package com.example.filetransfer.di

import android.content.Context
import com.example.filetransfer.data.p2p.WifiP2pDataSource

class AppContainer(
    context: Context
) {

    private val appContext = context.applicationContext

    val wifiP2pDataSource = WifiP2pDataSource(
        context = appContext
    )
}