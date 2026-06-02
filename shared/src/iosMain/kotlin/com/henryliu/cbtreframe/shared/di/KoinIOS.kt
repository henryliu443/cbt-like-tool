package com.henryliu.cbtreframe.shared.di

import com.henryliu.cbtreframe.shared.db.AppDatabase
import com.henryliu.cbtreframe.shared.db.DatabaseDriverFactory
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { 
        val driverFactory = DatabaseDriverFactory()
        AppDatabase(driverFactory.createDriver())
    }
    
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
