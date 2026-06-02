package com.henryliu.cbtreframe.shared

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidKeychainProvider(private val context: Context) : KeychainProvider {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "secure_keychain_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        try {
            val file = java.io.File(context.filesDir?.parent.orEmpty() + "/shared_prefs/secure_keychain_prefs.xml")
            if (file.exists()) file.delete()
            EncryptedSharedPreferences.create(
                context,
                "secure_keychain_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e2: Exception) {
            context.getSharedPreferences("fallback_keychain_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun load(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun save(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override fun deleteAll() {
        sharedPreferences.edit().clear().apply()
    }
}
