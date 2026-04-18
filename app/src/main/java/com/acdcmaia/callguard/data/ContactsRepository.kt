package com.acdcmaia.callguard.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    @Volatile private var cache: Map<String, String>? = null  // digits -> displayName
    private val cacheMutex = Mutex()

    init {
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) { cache = null }
            }
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
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val result = mutableMapOf<String, String>()

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val cd = cursor.getString(numIdx)?.filter { it.isDigit() } ?: continue
                val name = cursor.getString(nameIdx) ?: continue
                if (cd.length >= 4) result[cd] = name
            }
        }
        cache = result
        result
    }
}
