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
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class ContactsRepository(private val context: Context) {

    private val phoneUtil = PhoneNumberUtil.getInstance()
    private val defaultRegion: String
        get() = context.resources.configuration.locales.get(0).country.takeIf { it.isNotEmpty() } ?: "BR"

    private val cacheRef = AtomicReference<Map<String, String>?>(null)
    private val cacheMutex = Mutex()

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { cacheRef.set(null) }
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
            cacheRef.set(null)
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

    private fun toE164(raw: String): String? = try {
        val parsed = phoneUtil.parse(raw, defaultRegion)
        if (phoneUtil.isValidNumber(parsed))
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        else null
    } catch (_: Exception) { null }

    private suspend fun getCache(): Map<String, String> =
        cacheRef.get() ?: cacheMutex.withLock { cacheRef.get() ?: loadCache() }

    suspend fun isContact(number: String): Boolean = withContext(Dispatchers.IO) {
        val cache = getCache()
        val e164 = toE164(number)
        if (e164 != null && cache.containsKey(e164)) return@withContext true
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext false
        cache.containsKey(digits)
    }

    suspend fun getContactName(number: String): String? = withContext(Dispatchers.IO) {
        val cache = getCache()
        val e164 = toE164(number)
        if (e164 != null) cache[e164]?.let { return@withContext it }
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return@withContext null
        cache[digits]
    }

    private suspend fun loadCache(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) {
            cacheRef.set(emptyMap())
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
                    val raw = cursor.getString(numIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val key = toE164(raw) ?: raw.filter { it.isDigit() }.takeIf { it.length >= 4 } ?: continue
                    result[key] = name
                }
            }
        } catch (_: Exception) {}
        cacheRef.set(result)
        result
    }
}
