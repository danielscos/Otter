package com.example.otter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.otter.DownloadRepository
import com.example.otter.DownloaderScreen
import com.example.otter.DownloaderViewModel
import com.example.otter.ui.theme.OtterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // initialize the repository
        val repository = DownloadRepository(applicationContext)

        // initialize ViewModel using factory
        val viewModel = ViewModelProvider(this, object: ViewModelProvider.Factory {
            override fun <T: ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DownloaderViewModel(repository) as T
            }
        })[DownloaderViewModel::class.java]

        // handle share intent from tiktok/instagram
        var sharedUrl: String? = null

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            // tiktok shares text like check this out crap
            val fullText = intent.getStringExtra(Intent.EXTRA_TEXT)

            sharedUrl = fullText?.let { text ->
                Regex("https?://\\S+").find(text)?.value
            }
        }

        setContent {
            OtterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // pass the ViewModel and the optional shared url to the screen
                    DownloaderScreen(
                        viewModel = viewModel,
                        initialUrl = sharedUrl,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}