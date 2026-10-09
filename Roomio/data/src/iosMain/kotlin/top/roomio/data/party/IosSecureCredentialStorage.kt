package top.roomio.data.party

import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.errSecItemNotFound
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Firebase tokens are stored as a generic-password item protected by iOS Keychain. */
private class IosSecureCredentialStorage : SecureCredentialStorage {
    override fun read(): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query(returnData = true) as CFDictionaryRef, result.ptr)
        if (status == errSecItemNotFound) return null
        check(status == errSecSuccess) { "Keychain credential read failed (status $status)" }
        val data = result.value as? NSData ?: return null
        val bytes = data.bytes?.readBytes(data.length.toInt()) ?: return null
        bytes.decodeToString()
    }

    override fun write(value: String) {
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        val update = NSMutableDictionary().apply { setObject(data, forKey = kSecValueData) }
        val status = SecItemUpdate(query() as CFDictionaryRef, update as CFDictionaryRef)
        if (status == errSecItemNotFound) {
            val addAttributes = query().apply { setObject(data, forKey = kSecValueData) }
            val addStatus = SecItemAdd(addAttributes as CFDictionaryRef, null)
            check(addStatus == errSecSuccess) { "Keychain credential write failed (status $addStatus)" }
        } else {
            check(status == errSecSuccess) { "Keychain credential update failed (status $status)" }
        }
    }

    override fun clear() {
        SecItemDelete(query() as CFDictionaryRef)
    }

    private fun query(returnData: Boolean = false) = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword, forKey = kSecClass)
        setObject(SERVICE, forKey = kSecAttrService)
        setObject(ACCOUNT, forKey = kSecAttrAccount)
        setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, forKey = kSecAttrAccessible)
        if (returnData) {
            setObject(true, forKey = kSecReturnData)
            setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)
        }
    }

    private companion object {
        const val SERVICE = "top.roomio.firebase.credentials"
        const val ACCOUNT = "firebase-session-v1"
    }
}

internal actual fun createSecureCredentialStorage(): SecureCredentialStorage = IosSecureCredentialStorage()
