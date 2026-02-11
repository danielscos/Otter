package com.example.otter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyException
import io.github.junkfood02.youtubedl.YoutubeDL
import io.github.junkfood02.youtubedl.YoutubeDLException
import io.github.junkfood02.youtubedl.mapper.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val videoInfo: VideoInfo) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                YoutubeDL.getInstance().init(getApplication())
            } catch (e: YoutubeDLException) {
                _uiState.value = UiState.Error(e.message ?: "Failed to initialize YoutubeDL")
            }
        }
    }

    fun fetchVideoInfo(url: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val videoInfo = YoutubeDL.getInstance().getInfo(url)
                _uiState.value = UiState.Success(videoInfo)
            } catch (e: YoutubeDLException) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            } catch (e: PyException) {
                _uiState.value = UiState.Error(e.message ?: "A Python error occurred.")
            } catch (e: InterruptedException) {
                _uiState.value = UiState.Error("The request was interrupted.")
            }
        }
    }
}
