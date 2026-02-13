package com.example.otter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.otter.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// stack to track what is happening
data class DownloadState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val etaSeconds: Long = 0,
    val statusText: String = "",
    val downloadedFile: File? = null,
    val error: String? = null,
    val isDownloadComplete: Boolean = false
)

class DownloaderViewModel(private val repository: DownloadRepository) : ViewModel() {
    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    fun startDownload(url: String) {
        if (url.isBlank()) return

        viewModelScope.launch {
            _state.value = DownloadState(isLoading = true, statusText = "Initializing...")

            val result = repository.downloadVideo(url) { progress, eta, line ->
                // this block runs continously during the download
                val cleanStatus = if (progress > 0) {
                    "Downloading: ${progress.toInt()}%"
                } else {
                    // use the raw line from yt-dlp for detailed status
                    cleanLine(line)
                }

                _state.value = _state.value.copy(
                    progress = progress / 100f,
                    etaSeconds = eta,
                    statusText = cleanStatus
                )
            }

            result.fold(
                onSuccess = { file ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        progress = 1f,
                        isDownloadComplete = true,
                        downloadedFile = file,
                        statusText = "Complete!"
                    )

                    delay(1000)

                    // final state after animation
                    _state.value = _state.value.copy(
                        isLoading = false,
                        statusText = "Saved to gallery :3"
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Unknown error",
                        statusText = "failed .w."
                    )
                }
            )
        }
    }

    private fun cleanLine(line: String?): String {
        // removes "[download]" or "[ffmpeg]" prefixes to make it look clean
        val cleaned = line
            ?.replace(Regex("^\\[.*?\\]\\s*"), "")
            ?.take(40)
            ?.trim()
        return if (cleaned.isNullOrBlank()) "Processing..." else cleaned
    }
}