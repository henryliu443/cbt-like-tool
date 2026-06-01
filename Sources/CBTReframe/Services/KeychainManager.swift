import Foundation
#if !SKIP
import Security
#else
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
#endif

final class KeychainManager {
    static let shared = KeychainManager()
    private let service = "com.cbt.reframe.apikeys"

    #if SKIP
    private lazy var sharedPreferences: SharedPreferences = {
        let context = ProcessInfo.processInfo.androidContext
        let masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            service,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }()
    #endif

    private init() {}

    func save(key: String, value: String) {
        #if !SKIP
        guard let data = value.data(using: .utf8) else { return }
        delete(key: key)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        SecItemAdd(query as CFDictionary, nil)
        #else
        sharedPreferences.edit().putString(key, value).apply()
        #endif
    }

    func load(key: String) -> String? {
        #if !SKIP
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }
        return string
        #else
        return sharedPreferences.getString(key, nil)
        #endif
    }

    func delete(key: String) {
        #if !SKIP
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
        #else
        sharedPreferences.edit().remove(key).apply()
        #endif
    }

    func deleteAll() {
        for provider in AIProvider.allCases where provider.requiresAPIKey {
            delete(key: provider.rawValue)
        }
    }
}
