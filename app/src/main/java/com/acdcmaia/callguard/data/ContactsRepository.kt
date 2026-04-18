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

    @Volatile private var cache: Set<String>? = null
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

    suspend fun isContact(number: String): Boolean = withContext(Dispatchers.IO) {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext false

        val contactDigits = cache ?: cacheMutex.withLock { cache ?: loadCache() }
        contactDigits.any { cd ->
            digits.endsWith(cd.takeLast(8)) || cd.endsWith(digits.takeLast(8))
        }
    }

    private suspend fun loadCache(): Set<String> = withContext(Dispatchers.IO) {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val result = mutableSetOf<String>()

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val colIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val cd = cursor.getString(colIdx)?.filter { it.isDigit() } ?: continue
                if (cd.length >= 4) result.add(cd)
            }
        }
        cache = result
        result
    }
}
