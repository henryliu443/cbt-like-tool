package com.henryliu.cbtreframe.shared.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.henryliu.cbtreframe.shared.db.AppDatabase
import com.henryliu.cbtreframe.shared.db.DatabaseDriverFactory
import com.henryliu.cbtreframe.shared.KeychainProvider
import com.henryliu.cbtreframe.shared.AndroidKeychainProvider
import com.henryliu.cbtreframe.shared.ReminderScheduler
import com.henryliu.cbtreframe.shared.AndroidReminderScheduler
import com.henryliu.cbtreframe.shared.BiometricAuthProvider
import com.henryliu.cbtreframe.shared.AndroidBiometricAuthProvider
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { 
        val driverFactory = DatabaseDriverFactory(get())
        AppDatabase(driverFactory.createDriver())
    }
    
    single<KeychainProvider> { AndroidKeychainProvider(get()) }
    single<ReminderScheduler> { AndroidReminderScheduler(get()) }
    single<BiometricAuthProvider> { AndroidBiometricAuthProvider(get()) }

    
    single<Settings> {
        val context: Context = get()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        val sharedPreferences = try {
            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                // Delete the underlying file completely
                val file = java.io.File(context.filesDir?.parent.orEmpty() + "/shared_prefs/secret_shared_prefs.xml")
                if (file.exists()) file.delete()
                
                // Recreate
                EncryptedSharedPreferences.create(
                    context,
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                // Ultimate fallback for devices with broken Tink/Hardware Keystores
                context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
            }
        }
        SharedPreferencesSettings(sharedPreferences)
    }
}
