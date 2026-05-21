package com.acdcmaia.callguard.ui.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.io.File
import java.net.URL

enum class UpdateStatus {
    CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR,
    DOWNLOADING, READY_TO_INSTALL, DOWNLOAD_ERROR
}

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val windowSeconds: Flow<Int> = settings.windowSeconds
    val autostartConfigured: Flow<Boolean> = settings.autostartConfigured
    val serviceEnabled: Flow<Boolean> = settings.serviceEnabled

    fun markAutostartConfigured() {
        viewModelScope.launch { settings.setAutostartConfigured() }
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setServiceEnabled(enabled) }
    }

    private val _updateStatus = MutableStateFlow(UpdateStatus.CHECKING)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _apkUri = MutableStateFlow<Uri?>(null)
    val apkUri: StateFlow<Uri?> = _apkUri

    private var updateJob: Job? = null
    private var downloadUrl: String? = null

    fun checkForUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            _updateStatus.value = UpdateStatus.CHECKING
            try {
                val (latest, url) = supervisorScope {
                    val fetchDeferred = async(Dispatchers.IO) {
                        val json = URL("https://api.github.com/repos/acdcmaia/call-guard/releases/latest").readText()
                        val tag = Regex("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").find(json)
                            ?.groupValues?.get(1) ?: error("tag_name não encontrado")
                        val apkUrl = Regex(""""browser_download_url"\s*:\s*"([^"]+\.apk)"""").find(json)
                            ?.groupValues?.get(1)
                        Pair(tag, apkUrl)
                    }
                    delay(MIN_CHECKING_DURATION_MS)
                    fetchDeferred.await()
                }
                downloadUrl = url
                _updateStatus.value = if (isNewer(latest, BuildConfig.VERSION_NAME))
                    UpdateStatus.UPDATE_AVAILABLE else UpdateStatus.UP_TO_DATE
            } catch (_: Exception) {
                _updateStatus.value = UpdateStatus.ERROR
            }
        }
    }

    fun downloadUpdate(context: Context) {
        val url = downloadUrl ?: return
        if (!url.startsWith("https://github.com/acdcmaia/call-guard/")) return
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.DOWNLOADING
            _downloadProgress.value = 0

            val dm = context.getSystemService(DownloadManager::class.java)
            val destFile = File(context.getExternalFilesDir(null), "CallGuard-update.apk")
            if (destFile.exists()) destFile.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Call Guard")
                .setDescription("Baixando atualização…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, "CallGuard-update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = dm.enqueue(request)

            withTimeoutOrNull(10 * 60_000L) {
                while (true) {
                    delay(1_000)
                    val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId)) ?: continue
                    val done = cursor.use {
                        if (!it.moveToFirst()) return@use false
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total > 0) _downloadProgress.value = (downloaded * 100 / total).toInt()
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                _apkUri.value = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    destFile
                                )
                                _updateStatus.value = UpdateStatus.READY_TO_INSTALL
                                true
                            }
                            DownloadManager.STATUS_FAILED -> {
                                _updateStatus.value = UpdateStatus.DOWNLOAD_ERROR
                                true
                            }
                            else -> false
                        }
                    }
                    if (done) break
                }
            } ?: run { _updateStatus.value = UpdateStatus.DOWNLOAD_ERROR }
        }
    }

    fun onInstallTriggered() {
        _apkUri.value = null
        _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
    }

    fun setWindowSeconds(seconds: Int) {
        viewModelScope.launch { settings.setWindowSeconds(seconds) }
    }

    companion object {
        private const val MIN_CHECKING_DURATION_MS = 2_000L

        private fun isNewer(remote: String, local: String): Boolean {
            val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val l = local.split(".").map { it.toIntOrNull() ?: 0 }
            val size = maxOf(r.size, l.size)
            for (i in 0 until size) {
                val rv = r.getOrElse(i) { 0 }
                val lv = l.getOrElse(i) { 0 }
                if (rv != lv) return rv > lv
            }
            return false
        }

        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(app.settingsRepository) as T
        }
    }
}
