package com.needai.chat.ui.ilink

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.ui.theme.*
import com.needai.chat.util.FileLogger
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IlinkSetupScreen(
    viewModel: IlinkViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("接入微信 ClawBot") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgPage
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPage
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is IlinkUiState.Setup -> {
                    SetupContent(
                        state = state,
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        onPluginEnabled = viewModel::onPluginEnabled,
                        onOpenWeChat = {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    tryOpenWeChatFallback(context, snackbarHostState, scope)
                                }
                            } catch (_: Exception) {
                                tryOpenWeChatFallback(context, snackbarHostState, scope)
                            }
                        },
                        onComplete = {
                            viewModel.onSetupComplete()
                            onComplete()
                        }
                    )
                }
                is IlinkUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.reconnect() }
                    )
                }
                is IlinkUiState.Connected -> {
                    // 已授权，直接跳到管理页
                    LaunchedEffect(Unit) { onComplete() }
                }
                is IlinkUiState.Stopped -> {
                    // 已授权但桥接停止，也跳到管理页
                    LaunchedEffect(Unit) { onComplete() }
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun SetupContent(
    state: IlinkUiState.Setup,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onPluginEnabled: () -> Unit,
    onOpenWeChat: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            StepIndicator(step = 1, active = state.step.ordinal >= 0, label = "启用插件")
            Spacer(Modifier.width(32.dp))
            StepIndicator(step = 2, active = state.step.ordinal >= 1, label = "授权确认")
            Spacer(Modifier.width(32.dp))
            StepIndicator(step = 3, active = state.step.ordinal >= 2, label = "完成")
        }

        Spacer(Modifier.height(40.dp))

        when (state.step) {
            SetupStep.ENABLE_PLUGIN -> {
                Text(
                    text = "第一步：启用 ClawBot 插件",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "请打开微信，进入「我 → 设置 → 插件 → 微信 ClawBot」，点击启用。\n\n这是微信官方的 Bot 插件，安全可靠，无封号风险。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onOpenWeChat,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("打开微信", fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPluginEnabled,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("我已启用，下一步", fontSize = 16.sp)
                }
            }

            SetupStep.AUTHORIZE -> {
                val context = LocalContext.current
                val qrBitmap = produceState<Bitmap?>(initialValue = null, state.qrCodeUrl) {
                    value = state.qrCodeUrl?.let { generateQrBitmap(it) }
                }

                Text(
                    text = "第二步：授权确认",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "请将二维码保存到相册，然后在微信中扫码授权。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))

                // QR Code
                if (qrBitmap.value != null) {
                    Image(
                        bitmap = qrBitmap.value!!.asImageBitmap(),
                        contentDescription = "授权二维码",
                        modifier = Modifier.size(220.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save to gallery
                OutlinedButton(
                    onClick = {
                        FileLogger.i("SetupScreen", "保存二维码 按钮点击")
                        val bitmap = qrBitmap.value
                        if (bitmap != null) {
                            FileLogger.i("SetupScreen", "开始保存二维码到相册")
                            // clipboard fallback
                            try {
                                val clipMgr = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as? android.content.ClipboardManager
                                clipMgr?.setPrimaryClip(android.content.ClipData.newPlainText("auth_url", state.qrCodeUrl))
                            } catch (_: Exception) { }
                            if (saveQrCodeToGallery(context, bitmap)) {
                                FileLogger.i("SetupScreen", "二维码保存成功")
                                scope.launch { snackbarHostState.showSnackbar("二维码已保存到相册") }
                            } else {
                                FileLogger.w("SetupScreen", "二维码保存失败")
                                scope.launch { snackbarHostState.showSnackbar("保存失败，请重试") }
                            }
                        } else {
                            FileLogger.w("SetupScreen", "qrBitmap 为 null，无法保存")
                        }
                    },
                    enabled = qrBitmap.value != null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存二维码到相册", fontSize = 16.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Open WeChat (复用 Step 1 的 onOpenWeChat，自带 weixin:// 兜底)
                Button(
                    onClick = {
                        FileLogger.i("SetupScreen", "打开微信 按钮点击")
                        // clipboard fallback
                        try {
                            val clipMgr = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as? android.content.ClipboardManager
                            clipMgr?.setPrimaryClip(android.content.ClipData.newPlainText("auth_url", state.qrCodeUrl))
                        } catch (_: Exception) { }
                        onOpenWeChat()
                        scope.launch { snackbarHostState.showSnackbar("请在微信中扫一扫 → 相册选择二维码") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("打开微信", fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Polling status
                if (state.qrCodeUrl != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BrandBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "等待确认中...",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }

            SetupStep.COMPLETE -> {
                Text(
                    text = "✅ 接入成功！",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "微信 ClawBot 已连接成功。\n现在你的微信好友可以直接跟「${state.currentSkill?.name ?: "当前角色"}」对话了。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("开始使用", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (active) BrandBlue else GlassWhite,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "$step",
                    color = if (active) Color.White else TextTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = if (active) TextPrimary else TextTertiary)
    }
}

/** 尝试通过 weixin:// 协议打开微信，失败时显示 Snackbar */
private fun tryOpenWeChatFallback(
    context: android.content.Context,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    try {
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://"))
        context.startActivity(fallback)
    } catch (_: Exception) {
        scope.launch {
            snackbarHostState.showSnackbar("请确认已安装微信")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "出错了", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text(text = message, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}
