package com.henryliu.cbtreframe.shared

interface BiometricAuthProvider {
    fun canAuthenticate(): Boolean
    suspend fun authenticate(promptTitle: String, promptSubtitle: String): Boolean
}
