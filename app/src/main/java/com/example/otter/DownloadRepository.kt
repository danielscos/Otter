package com.example.otter

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 *                                       author:
 *
 * ▓█████▄  ▄▄▄       ███▄    █  ██▓▓█████  ██▓      ██████  ▄████▄   ▒█████    ██████
 * ▒██▀ ██▌▒████▄     ██ ▀█   █ ▓██▒▓█   ▀ ▓██▒    ▒██    ▒ ▒██▀ ▀█  ▒██▒  ██▒▒██    ▒
 * ░██   █▌▒██  ▀█▄  ▓██  ▀█ ██▒▒██▒▒███   ▒██░    ░ ▓██▄   ▒▓█    ▄ ▒██░  ██▒░ ▓██▄
 * ░▓█▄   ▌░██▄▄▄▄██ ▓██▒  ▐▌██▒░██░▒▓█  ▄ ▒██░      ▒   ██▒▒▓▓▄ ▄██▒▒██   ██░  ▒   ██▒
 * ░▒████▓  ▓█   ▓██▒▒██░   ▓██░░██░░▒████▒░██████▒▒██████▒▒▒ ▓███▀ ░░ ████▓▒░▒██████▒▒
 *  ▒▒▓  ▒  ▒▒   ▓▒█░░ ▒░   ▒ ▒ ░▓  ░░ ▒░ ░░ ▒░▓  ░▒ ▒▓▒ ▒ ░░ ░▒ ▒  ░░ ▒░▒░▒░ ▒ ▒▓▒ ▒ ░
 *  ░ ▒  ▒   ▒   ▒▒ ░░ ░░   ░ ▒░ ▒ ░ ░ ░  ░░ ░ ▒  ░░ ░▒  ░ ░  ░  ▒     ░ ▒ ▒░ ░ ░▒  ░ ░
 *  ░ ░  ░   ░   ▒      ░   ░ ░  ▒ ░   ░     ░ ░   ░  ░  ░  ░        ░ ░ ░ ▒  ░  ░  ░
 *    ░          ░  ░         ░  ░     ░  ░    ░  ░      ░  ░ ░          ░ ░        ░
 *  ░                                                       ░
 */
class DownloadRepository(private val context: Context) {

    suspend fun downloadVideo(
        url: String,
        onProgress: (Float, Long, String?) -> Unit
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            val appCacheDir = File(context.externalCacheDir, "youtubedl-cache")
            if (!appCacheDir.exists())
                appCacheDir.mkdirs()

            val request = YoutubeDLRequest(url)
            request.addOption("-o", "${appCacheDir.absolutePath}/%(title)s.%(ext)s")
            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
            request.addOption("--no-mtime")

            val versionRequest = YoutubeDLRequest("")
            versionRequest.addOption("--version")
            val response = YoutubeDL.getInstance().execute(versionRequest)
            val versionOutput = response.out
            Log.d("YTDL", "yt-dlp version: $versionOutput")

            try {
                YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                    onProgress(progress, eta, line)
                }

                val downloadedFile = appCacheDir.listFiles()?.maxByOrNull { it.lastModified() } ?: throw Exception("Download finished but file not found in cache.")

                val galleryUri = saveToGallery(downloadedFile)

                if (galleryUri != null) {
                    downloadedFile.delete()
                    Result.success(downloadedFile)
                } else {
                    Result.failure(Exception("failed to save to gallery"))
                }
            } catch (e: Exception) {
                Log.e("DownloadRepo", "Download failed\n", e)
                Result.failure(e)
            }
        }
    }

    private fun saveToGallery(file: File): String? {
        val fileName = file.name
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)

            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)

                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/otter")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            null
        }
    }
}