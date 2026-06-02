package com.henryliu.cbtreframe

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.henryliu.cbtreframe.shared.BiometricResultReceiver

class BiometricActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Return false if someone started this activity without a receiver
        if (BiometricResultReceiver.deferredResult == null) {
            finish()
            return
        }

        val promptTitle = intent.getStringExtra("promptTitle") ?: "Authenticate"
        val promptSubtitle = intent.getStringExtra("promptSubtitle") ?: ""

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    BiometricResultReceiver.deferredResult?.complete(false)
                    BiometricResultReceiver.deferredResult = null
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    BiometricResultReceiver.deferredResult?.complete(true)
                    BiometricResultReceiver.deferredResult = null
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't finish on failed, prompt stays visible to retry
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText("Cancel")
            // Depending on configuration, BIOMETRIC_WEAK might be used
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
