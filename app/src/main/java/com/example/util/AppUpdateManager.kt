package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    data class Downloading(val progress: Int = 0) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadId: Long = -1L
    private var isReceiverRegistered = false

    private val onDownloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(recvContext: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (id == downloadId && downloadId != -1L) {
                handleDownloadCompletion()
            }
        }
    }

    fun startDownload(downloadUrl: String = DEFAULT_DOWNLOAD_URL) {
        try {
            _updateState.value = UpdateState.Downloading(0)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                _updateState.value = UpdateState.Error("Download Manager service unavailable.")
                return
            }

            // Create target file inside application's cache directory or external files
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expense_manager_update.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading Expense Tracker Update")
                setDescription("Fetching the latest APK build...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            if (!isReceiverRegistered) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        onDownloadCompleteReceiver,
                        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(
                        onDownloadCompleteReceiver,
                        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    )
                }
                isReceiverRegistered = true
            }

            downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Download started in background...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to start download: ${e.localizedMessage}")
        }
    }

    private fun handleDownloadCompletion() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex != -1) {
                val status = cursor.getInt(statusIndex)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expense_manager_update.apk")
                    if (destinationFile.exists()) {
                        _updateState.value = UpdateState.ReadyToInstall(destinationFile)
                        installApk(destinationFile)
                    } else {
                        _updateState.value = UpdateState.Error("Downloaded file not found.")
                    }
                } else {
                    _updateState.value = UpdateState.Error("Download failed with status $status")
                }
            }
            cursor.close()
        }
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("APK file missing.")
                return
            }

            // Check unknown app sources permission for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Please allow 'Install unknown apps' permission to update", Toast.LENGTH_LONG).show()
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to launch installer: ${e.localizedMessage}")
        }
    }

    fun cleanup() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(onDownloadCompleteReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    companion object {
        const val DEFAULT_DOWNLOAD_URL = "https://github.com/Akandulna/Exp-Manager-2.0/releases/latest/download/app-debug.apk"
    }
}
