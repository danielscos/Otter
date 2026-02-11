package com.example.otter

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object FileUtils {
    fun moveToGallery(context: Context, sourceFile: File, title: String): Boolean {
        // create container for video info
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, title)

            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            // mark as pending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)

                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/otter")
            }
        }

        // insert into mediastore (the database of all the media on the phone)
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false

        try {
            // open the output stream to the gallery and write our file
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            sourceFile.delete()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // clean up if it failed
            resolver.delete(uri, null, null)
            return false

        }
    }
}