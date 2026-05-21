package com.needai.chat.ui.util

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

enum class ToastType { Success, Error, Info }

data class ToastItem(
    val id: Long,
    val message: String,
    val type: ToastType
)

class ToastState {
    private val _toasts = SnapshotStateList<ToastItem>()
    val toasts: List<ToastItem> get() = _toasts

    private val idCounter = AtomicLong(0)

    fun show(message: String, type: ToastType = ToastType.Success) {
        val id = idCounter.incrementAndGet()
        _toasts.add(ToastItem(id, message, type))
    }

    fun dismiss(id: Long) {
        _toasts.removeAll { it.id == id }
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

@Composable
fun AppToastHost(
    modifier: Modifier = Modifier
) {
    val state = LocalToast.current

    // Auto-dismiss via LaunchedEffect per toast
    for (toast in state.toasts) {
        val id = toast.id
        LaunchedEffect(id) {
            delay(2500)
            state.dismiss(id)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp)
                .widthIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (toast in state.toasts) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    ToastView(toast)
                }
            }
        }
    }
}

@Composable
private fun ToastView(item: ToastItem) {
    val colors = when (item.type) {
        ToastType.Success -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
        ToastType.Error -> Color(0xFFEF5350) to Color(0xFFFFEBEE)
        ToastType.Info -> Color(0xFF5B9DFF) to Color(0xFFE3F2FD)
    }

    Box(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.first)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = item.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111318)
            )
        }
    }
}
