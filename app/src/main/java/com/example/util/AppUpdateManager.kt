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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateState {
    object Idle : UpdateState()
    data class Checking(val message: String = "Connecting to GitHub Releases...") : UpdateState()
    data class Downloading(val progress: Int = 0, val downloadedMb: String = "", val totalMb: String = "", val fileName: String = "app-debug.apk") : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String, val canOpenBrowser: Boolean = true, val is404NoRelease: Boolean = false) : UpdateState()
}

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _hasNewUpdate = MutableStateFlow(false)
    val hasNewUpdate: StateFlow<Boolean> = _hasNewUpdate.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var downloadJob: Job? = null

    fun checkForUpdateSilently() {
        scope.launch(Dispatchers.IO) {
            try {
                val releaseInfo = fetchLatestReleaseMetadata()
                if (releaseInfo != null && releaseInfo.hasApk) {
                    val isNewer = isReleaseNewer(releaseInfo.tagName, releaseInfo.publishedAt)
                    _hasNewUpdate.value = isNewer
                } else {
                    _hasNewUpdate.value = false
                }
            } catch (_: Exception) {
                _hasNewUpdate.value = false
            }
        }
    }

    data class ReleaseMetadata(
        val tagName: String,
        val publishedAt: String,
        val hasApk: Boolean,
        val apkDownloadUrl: String?
    )

    private fun fetchLatestReleaseMetadata(): ReleaseMetadata? {
        val endpoints = listOf(
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases/latest",
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases/tags/latest",
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ExpenseTracker/2.0")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val meta = parseReleaseMetadata(response)
                    if (meta != null) return meta
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseReleaseMetadata(jsonStr: String): ReleaseMetadata? {
        try {
            val trimmed = jsonStr.trim()
            val releaseObj = if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                if (array.length() > 0) array.getJSONObject(0) else null
            } else if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else null

            if (releaseObj != null) {
                val tagName = releaseObj.optString("tag_name", "")
                val publishedAt = releaseObj.optString("published_at", "")
                var hasApk = false
                var apkUrl: String? = null

                if (releaseObj.has("assets")) {
                    val assets = releaseObj.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            hasApk = true
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }

                return ReleaseMetadata(
                    tagName = tagName,
                    publishedAt = publishedAt,
                    hasApk = hasApk,
                    apkDownloadUrl = apkUrl
                )
            }
        } catch (_: Exception) {}
        return null
    }

    private fun isReleaseNewer(remoteTag: String, publishedAt: String): Boolean {
        try {
            val currentVersionName = com.example.BuildConfig.VERSION_NAME
            val currentCode = com.example.BuildConfig.VERSION_CODE

            val cleanRemote = remoteTag.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersionName.removePrefix("v").removePrefix("V").trim()

            // If tag has explicit version numbers like 1.0.2
            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            if (remoteParts.isNotEmpty() && currentParts.isNotEmpty()) {
                val maxLen = maxOf(remoteParts.size, currentParts.size)
                for (i in 0 until maxLen) {
                    val r = remoteParts.getOrElse(i) { 0 }
                    val c = currentParts.getOrElse(i) { 0 }
                    if (r > c) return true
                    if (r < c) return false
                }
                return false
            }

            // If remote tag is "latest", check if release was published after current app install time
            if (remoteTag.equals("latest", ignoreCase = true) && publishedAt.isNotBlank()) {
                try {
                    val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val lastUpdateTime = pkgInfo.lastUpdateTime
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val remoteDate = sdf.parse(publishedAt)?.time ?: 0L
                    // If release on github is newer than when the app package was installed
                    return remoteDate > lastUpdateTime
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return false
    }

    fun startDownload(customUrl: String? = null) {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Checking("Connecting to GitHub Releases...")

        downloadJob = scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    downloadApkWithFallback(customUrl)
                }
                _updateState.value = UpdateState.ReadyToInstall(apkFile)
                installApk(apkFile)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unknown error"
                val is404 = msg.contains("404") || msg.contains("No release APK asset found")
                val friendlyMessage = when {
                    is404 ->
                        "Could not connect to GitHub release APK. Please check your network or open the releases page in browser."
                    msg.contains("Unable to resolve host") || msg.contains("ConnectException") ->
                        "No internet connection. Please verify your network and retry."
                    else ->
                        "Download failed: $msg"
                }
                _updateState.value = UpdateState.Error(friendlyMessage, canOpenBrowser = true, is404NoRelease = is404)
            }
        }
    }

    private fun downloadApkWithFallback(customUrl: String?): File {
        val candidateUrls = mutableListOf<String>()

        if (!customUrl.isNullOrBlank()) {
            candidateUrls.add(customUrl.trim())
        }

        // Direct tag URL (Matches tag: latest in your GitHub Actions release)
        candidateUrls.add("https://github.com/Akandulna/Exp-Manager-2.0/releases/download/latest/app-debug.apk")
        
        // GitHub API fetched URLs
        candidateUrls.addAll(fetchApkUrlsFromGithubApi())

        // Standard latest alias
        candidateUrls.add("https://github.com/Akandulna/Exp-Manager-2.0/releases/latest/download/app-debug.apk")

        var lastError: Exception? = null

        for (url in candidateUrls.distinct()) {
            try {
                return downloadFromUrl(url)
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("No release APK asset found at available URLs")
    }

    private fun fetchApkUrlsFromGithubApi(): List<String> {
        val result = mutableListOf<String>()
        val endpoints = listOf(
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases/tags/latest",
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases/latest",
            "https://api.github.com/repos/Akandulna/Exp-Manager-2.0/releases"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ExpenseTracker/2.0")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val urls = parseApkUrls(response)
                    result.addAll(urls)
                    if (result.isNotEmpty()) break
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {}
        }
        return result
    }

    private fun parseApkUrls(jsonStr: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val rel = array.getJSONObject(i)
                    extractAssets(rel, urls)
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                extractAssets(obj, urls)
            }
        } catch (_: Exception) {}
        return urls
    }

    private fun extractAssets(rel: JSONObject, urls: MutableList<String>) {
        if (!rel.has("assets")) return
        val assets = rel.getJSONArray("assets")
        for (j in 0 until assets.length()) {
            val asset = assets.getJSONObject(j)
            val name = asset.optString("name", "")
            val downloadUrl = asset.optString("browser_download_url", "")
            if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                urls.add(downloadUrl)
            }
        }
    }

    private fun downloadFromUrl(initialUrl: String): File {
        var currentUrl = initialUrl
        var redirectCount = 0
        val maxRedirects = 10
        var connection: HttpURLConnection? = null

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 40000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ExpenseTracker/2.0")
                setRequestProperty("Accept", "*/*")
            }

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("Redirect without location")
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
                connection.disconnect()
                throw IllegalStateException("HTTP $responseCode from $currentUrl")
            }
        }

        val conn = connection ?: throw IllegalStateException("Could not open connection")
        val fileLength = conn.contentLengthLong

        val targetFile = File(context.cacheDir, "expense_tracker_update.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            input = conn.inputStream
            output = FileOutputStream(targetFile)

            val buffer = ByteArray(16 * 1024)
            var totalBytesRead: Long = 0
            var bytesRead: Int
            var lastReportedProgress = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val progress = if (fileLength > 0) {
                    ((totalBytesRead * 100) / fileLength).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                if (progress != lastReportedProgress) {
                    lastReportedProgress = progress
                    val downloadedMb = String.format(java.util.Locale.US, "%.1f MB", totalBytesRead / (1024.0 * 1024.0))
                    val totalMb = if (fileLength > 0) String.format(java.util.Locale.US, "%.1f MB", fileLength / (1024.0 * 1024.0)) else "30.2 MB"
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

        if (targetFile.length() < 10000) {
            throw IllegalStateException("Downloaded file is invalid (${targetFile.length()} bytes)")
        }

        return targetFile
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("APK file not found.", canOpenBrowser = true)
                return
            }

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

    fun resetState() {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Idle
    }

    companion object {
        const val GITHUB_RELEASES_PAGE_URL = "https://github.com/Akandulna/Exp-Manager-2.0/releases"
    }
}
