package com.henryliu.cbtreframe.shared.security

actual class PlatformKeyStore actual constructor() {
    actual fun saveKey(alias: String, key: ByteArray) {
        // TODO: Implement basic Android stub
    }

    actual fun getKey(alias: String): ByteArray? {
        // TODO: Implement basic Android stub
        return null
    }
}
