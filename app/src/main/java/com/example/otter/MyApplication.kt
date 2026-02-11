package com.example.otter

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // initialize in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // unpacking python + executables
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                Log.d("MyApplication", "engine initialized :3")

            } catch (e: YoutubeDLException) {
                Log.e("MyApplication", "error initializing engine :<\n", e)

                // notify user too about this
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext, "Error initializing engine", Toast.LENGTH_LONG).show()
                }
            }

        }
    }
}