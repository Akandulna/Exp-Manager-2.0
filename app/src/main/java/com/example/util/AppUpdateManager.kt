package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateState {
    object Idle : UpdateState()
    data class Downloading(val progress: Int = 0, val downloadedMb: String = "", val totalMb: String = "") : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String, val canOpenBrowser: Boolean = true) : UpdateState()
}

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var downloadJob: Job? = null

    fun startDownload(downloadUrl: String = DEFAULT_DOWNLOAD_URL) {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Downloading(0, "0 MB", "...")

        downloadJob = scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    downloadApkWithRedirects(downloadUrl)
                }
                _updateState.value = UpdateState.ReadyToInstall(apkFile)
                installApk(apkFile)
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("404") == true ->
                        "Latest release APK not found on GitHub (404). Please ensure 'app-debug.apk' is published to GitHub Releases."
                    e.message?.contains("Unable to resolve host") == true ->
                        "No internet connection. Please check your network and try again."
                    else ->
                        "Download failed: ${e.localizedMessage ?: "Unknown error"}"
                }
                _updateState.value = UpdateState.Error(errorMessage, canOpenBrowser = true)
            }
        }
    }

    private fun downloadApkWithRedirects(initialUrl: String): File {
        var currentUrl = initialUrl
        var redirectCount = 0
        val maxRedirects = 6
        var connection: HttpURLConnection? = null

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ExpenseManager/2.0")
                setRequestProperty("Accept", "application/vnd.android.package-archive, */*")
            }

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("Redirect without Location header")
                connection.disconnect()
                currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                    location
                } else {
                    URL(url, location).toString()
                }
                redirectCount++
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                break
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                connection.disconnect()
                throw IllegalStateException("Server returned HTTP $responseCode ${errorStream?.take(100) ?: ""}")
            }
        }

        val conn = connection ?: throw IllegalStateException("Could not establish connection")
        val fileLength = conn.contentLengthLong

        // Save to cache dir which is accessible to FileProvider
        val targetFile = File(context.cacheDir, "expense_tracker_update.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            input = conn.inputStream
            output = FileOutputStream(targetFile)

            val buffer = ByteArray(8 * 1024)
            var totalBytesRead: Long = 0
            var bytesRead: Int
            var lastReportedProgress = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val progress = if (fileLength > 0) {
                    ((totalBytesRead * 100) / fileLength).toInt()
                } else {
                    0
                }

                if (progress != lastReportedProgress) {
                    lastReportedProgress = progress
                    val downloadedMb = String.format(java.util.Locale.US, "%.1f MB", totalBytesRead / (1024.0 * 1024.0))
                    val totalMb = if (fileLength > 0) String.format(java.util.Locale.US, "%.1f MB", fileLength / (1024.0 * 1024.0)) else "..."
                    scope.launch(Dispatchers.Main) {
                        _updateState.value = UpdateState.Downloading(progress, downloadedMb, totalMb)
                    }
                }
            }
            output.flush()
        } finally {
            try { output?.close() } catch (_: Exception) {}
            try { input?.close() } catch (_: Exception) {}
            conn.disconnect()
        }

        if (targetFile.length() < 1024) {
            throw IllegalStateException("Downloaded file is too small (${targetFile.length()} bytes)")
        }

        return targetFile
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("APK file not found.", canOpenBrowser = true)
                return
            }

            // Check unknown app sources permission for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Please enable 'Install unknown apps' permission to update", Toast.LENGTH_LONG).show()
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
            _updateState.value = UpdateState.Error("Failed to launch installer: ${e.localizedMessage}", canOpenBrowser = true)
        }
    }

    fun openBrowserReleases() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_PAGE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanup() {
        downloadJob?.cancel()
    }

    companion object {
        const val DEFAULT_DOWNLOAD_URL = "https://github.com/Akandulna/Exp-Manager-2.0/releases/latest/download/app-debug.apk"
        const val GITHUB_RELEASES_PAGE_URL = "https://github.com/Akandulna/Exp-Manager-2.0/releases"
    }
}
