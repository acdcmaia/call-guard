package com.acdcmaia.callguard.data

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun isContact(number: String): Boolean = withContext(Dispatchers.IO) {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext false

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val colIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val contactDigits = cursor.getString(colIdx)?.filter { it.isDigit() } ?: continue
                if (contactDigits.length >= 4 && (digits.endsWith(contactDigits.takeLast(8)) ||
                            contactDigits.endsWith(digits.takeLast(8)))) {
                    return@withContext true
                }
            }
        }
        false
    }

    companion object {
        @Volatile private var instance: ContactsRepository? = null

        fun getInstance(context: Context): ContactsRepository =
            instance ?: synchronized(this) {
                instance ?: ContactsRepository(context).also { instance = it }
            }
    }
}
