package com.example.otter

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DownloaderScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloaderViewModel,
    initialUrl: String? = null
) {
    var urlInput by remember { mutableStateOf(initialUrl ?: "") }
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // header
        Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Otter",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Paste a TikTok link to start",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // input field
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("https://platform.com/video/id") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = {
                    // paste from clipboard
                    clipboardManager.getText()?.let { urlInput = it.text }
                }) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = "Paste"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // action button
        Button(
            onClick = { viewModel.startDownload(urlInput) },
            enabled = urlInput.isNotBlank() && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Processing...")
            } else {
                Text("Download", fontSize = 18.sp)
            }
        }

        // status and progress
        Spacer(modifier = Modifier.height(24.dp))

        // transition Loading -> Success
        AnimatedContent(
            targetState = state.isDownloadComplete,
            transitionSpec = {
                if (targetState) {
                    // transition to Success:
                    // wait 500ms total before showing checkmark

                    (fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = 500
                        )
                    ) + scaleIn(initialScale = 0.8f, animationSpec = tween(300, delayMillis = 500)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(durationMillis = 300))
                        )
                } else {
                    fadeIn().togetherWith(fadeOut())
                }
            },
            label = "DownloadStatus"
        ) { isComplete ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isComplete) {
                    // success icon
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Download Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = (-6).dp)
                    )
                } else {
                    // progress bar

                    // keep the layour space reserved or handle empty state

                    if (state.isLoading) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (state.progress > 0) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text(
                                    text = "${(state.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // status text
        if (state.statusText.isNotBlank()) {
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // error display
        if (state.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
