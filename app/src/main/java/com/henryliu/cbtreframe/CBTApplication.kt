package com.henryliu.cbtreframe

import android.app.Application
import com.henryliu.cbtreframe.shared.BiometricAuthProvider
import com.henryliu.cbtreframe.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module

val appModule = module {
    single<BiometricAuthProvider> { AndroidBiometricAuthProvider(get()) }
}

class CBTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@CBTApplication)
            modules(appModule)
        }
    }
}
