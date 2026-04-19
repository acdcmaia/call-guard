package com.acdcmaia.callguard.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    @Volatile private var cache: Map<String, String>? = null  // digits -> displayName
    private val cacheMutex = Mutex()

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { cache = null }
    }

    init {
        // Observer vive enquanto o processo viver — intencional, pois ContactsRepository
        // é singleton em CallGuardApp e deve acompanhar o ciclo de vida do processo.
        // Só registra se READ_CONTACTS já foi concedida; caso contrário, registerPermission()
        // deve ser chamado após o usuário conceder a permissão.
        if (hasContactsPermission()) registerObserver()
    }

    fun registerPermission() {
        if (hasContactsPermission()) {
            cache = null
            registerObserver()
        }
    }

    private fun hasContactsPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

    private fun registerObserver() {
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI, true, contactsObserver
        )
    }

    private suspend fun getCache(): Map<String, String> =
        cache ?: cacheMutex.withLock { cache ?: loadCache() }

    suspend fun isContact(number: String): Boolean = withContext(Dispatchers.IO) {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext false
        getCache().keys.any { cd ->
            digits.endsWith(cd.takeLast(8)) || cd.endsWith(digits.takeLast(8))
        }
    }

    suspend fun getContactName(number: String): String? = withContext(Dispatchers.IO) {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext null
        getCache().entries.firstOrNull { (cd, _) ->
            digits.endsWith(cd.takeLast(8)) || cd.endsWith(digits.takeLast(8))
        }?.value
    }

    private suspend fun loadCache(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) {
            cache = emptyMap()
            return@withContext emptyMap()
        }
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val result = mutableMapOf<String, String>()

        val queryArgs = Bundle().apply {
            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 5_000)
        }
        try {
            context.contentResolver.query(uri, projection, queryArgs, null)?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val cd = cursor.getString(numIdx)?.filter { it.isDigit() } ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    if (cd.length >= 4) result[cd] = name
                }
            }
        } catch (_: Exception) {}
        cache = result
        result
    }
}
