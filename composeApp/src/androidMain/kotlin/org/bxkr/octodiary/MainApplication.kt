package org.bxkr.octodiary

import android.app.Application
import org.bxkr.octodiary.di.KoinApp
import org.koin.android.ext.koin.androidContext
import org.koin.plugin.module.dsl.startKoin

class MainApplication : Application() {

    companion object {
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin<KoinApp> {
            androidContext(this@MainApplication)
        }
    }
}