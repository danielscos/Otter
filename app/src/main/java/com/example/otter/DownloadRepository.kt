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
 *                                   author:
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

    /**
     * downloads a video from the given URL and saves it to the gallery.
     * @param url the tiktok url.
     * @param onProgress callback (progress 0-100 ETA in seconds, status exit)
     * @return result containing the final file path or error.
     */
    suspend fun downloadVideo(
        url: String,
        onProgress: (Float, Long, String?) -> Unit
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            // setup specific download dir in the app private cache
            // reason: cant write directly to gallery with yt-dlp

            val appCacheDir = File(context.externalCacheDir, "youtubedl-cache")
            if (!appCacheDir.exists())
                appCacheDir.mkdirs()

            // configure request
            val request = YoutubeDLRequest(url)
            request.addOption("-o", "${appCacheDir.absolutePath}/%(title)s.%(ext)s")

            // force mp4 format (ensures compatibility)
            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")

            request.addOption("--no-mtime") // use current file time so it appears at the top of the gallery
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // bypass basic anti-bot

            try {
                // execute the download

                YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                    // line = current status output from yt-dlp CLI
                    onProgress(progress, eta, line)
                }

                // find the file we downloaded
                // scan the dir for the most recently modified file
                val downloadeFile = appCacheDir.listFiles()?.maxByOrNull { it.lastModified() } ?: throw Exception("Download finished but file not found in cache.")

                val galleryUri = saveToGallery(downloadeFile)

                if (galleryUri != null) {
                    // delete cache file
                    downloadeFile.delete()
                    Result.success(downloadeFile)
                } else {
                    Result.failure(Exception("failed to save to gallery"))
                }
            } catch (e: Exception) {
                Log.e("DownloadRepo", "Download failed\n", e)
                Result.failure(e)
            }
        }
    }

    /**
     * internal helper to move a file from a private cache to public gallery (MediaStore)
     */
    private fun saveToGallery(file: File): String? {
        val fileName = file.name
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)

            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

            // for android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1) // mark as pending so gallery wont break things .w.

                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/otter")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        return try {
            // stream copy
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // finish up
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0) // you can scan now :D
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