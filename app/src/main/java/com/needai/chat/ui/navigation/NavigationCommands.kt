package com.needai.chat.ui.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Activity 到 Compose NavHost 的导航事件总线。
 *
 * MainActivity 通过 [navigate] 发送路由，
 * MainScreen 中 LaunchedEffect 收集 [flow] 执行导航。
 */
object NavigationCommands {
    private val _channel = Channel<String>(Channel.BUFFERED)
    val flow: Flow<String> = _channel.receiveAsFlow()

    fun navigate(route: String) {
        _channel.trySend(route)
    }
}
