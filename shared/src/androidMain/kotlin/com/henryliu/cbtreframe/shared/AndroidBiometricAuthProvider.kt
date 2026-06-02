package com.henryliu.cbtreframe.shared

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import kotlinx.coroutines.CompletableDeferred

object BiometricResultReceiver {
    var deferredResult: CompletableDeferred<Boolean>? = null
}

class AndroidBiometricAuthProvider(private val context: Context) : BiometricAuthProvider {
    override fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(promptTitle: String, promptSubtitle: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        BiometricResultReceiver.deferredResult = deferred

        val intent = Intent().apply {
            setClassName(context, "com.henryliu.cbtreframe.BiometricActivity")
            putExtra("promptTitle", promptTitle)
            putExtra("promptSubtitle", promptSubtitle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        return try {
            deferred.await()
        } finally {
            BiometricResultReceiver.deferredResult = null
        }
    }
}
