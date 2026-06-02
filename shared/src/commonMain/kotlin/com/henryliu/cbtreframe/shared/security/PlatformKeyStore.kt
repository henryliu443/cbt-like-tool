package com.henryliu.cbtreframe.shared.security

expect class PlatformKeyStore() {
    fun saveKey(alias: String, key: ByteArray)
    fun getKey(alias: String): ByteArray?
}
