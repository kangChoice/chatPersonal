package com.needai.chat.ui.voice.components

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoicePreviewPlayer(
    previewAudioBase64: String?,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var customText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "试听音色",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = customText,
            onValueChange = { customText = it },
            label = { Text("试听文本（可选）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入自定义试听文本...") }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isPlaying) onStop()
                    else onPlay(customText.ifBlank { "你好，欢迎试听我的声音。" })
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "播放"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPlaying) "停止" else "播放")
            }

            if (previewAudioBase64 != null) {
                Text(
                    text = "有预览音频",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
