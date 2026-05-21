package com.needai.chat.ui.ilink

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.data.ilink.IlinkClient
import com.needai.chat.data.ilink.IlinkScheduleManager
import com.needai.chat.data.ilink.QrCodeStatus
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class IlinkUiState {
    data object Loading : IlinkUiState()

    /** 已有 Token，已连接或可立即连接 */
    data class Connected(val skillName: String) : IlinkUiState()

    /** 桥接已停止 */
    data class Stopped(val skillName: String) : IlinkUiState()

    /** 首次引导 - 步骤 */
    data class Setup(
        val step: SetupStep = SetupStep.ENABLE_PLUGIN,
        val qrCodeUrl: String? = null,
        val currentSkill: Skill? = null
    ) : IlinkUiState()

    data class Error(val message: String) : IlinkUiState()
}

enum class SetupStep {
    ENABLE_PLUGIN,  // 引导启用 ClawBot 插件
    AUTHORIZE,      // 授权确认（显示 URL/QR）
    COMPLETE        // 完成
}

@HiltViewModel
class IlinkViewModel @Inject constructor(
    private val ilinkClient: IlinkClient,
    private val authManager: IlinkAuthManager,
    private val skillRepository: SkillRepository,
    private val scheduleManager: IlinkScheduleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<IlinkUiState>(IlinkUiState.Loading)
    val uiState: StateFlow<IlinkUiState> = _uiState.asStateFlow()

    /** 今日定时消息预览 */
    private val _todaySchedule = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val todaySchedule: StateFlow<List<Pair<String, String>>> = _todaySchedule.asStateFlow()

    /** 所有可选角色列表（用于切换） */
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    init {
        checkAuthAndLoad()
        loadSchedulePreview()
    }

    private fun loadSchedulePreview() {
        viewModelScope.launch {
            scheduleManager.initialize()
            _todaySchedule.value = scheduleManager.getTodaySchedulePreview()
        }
    }

    companion object {
        private const val TAG = "IlinkVM"
    }

    private fun checkAuthAndLoad() {
        viewModelScope.launch {
            skillRepository.getAllSkills().collect { skillList ->
                _skills.value = skillList
                FileLogger.i(TAG, "角色列表加载完成: ${skillList.size} 个")
            }
        }
        viewModelScope.launch {
            val currentSkill = getCurrentSkill()
            val skillName = currentSkill?.name ?: "未选择"
            val authed = authManager.isAuthenticated()
            FileLogger.i(TAG, "checkAuthAndLoad: authed=$authed, skill=$skillName")

            if (authed) {
                _uiState.value = IlinkUiState.Connected(skillName = skillName)
                startBridge()
            } else {
                _uiState.value = IlinkUiState.Setup(
                    step = SetupStep.ENABLE_PLUGIN,
                    currentSkill = currentSkill
                )
            }
        }
    }

    /** 用户确认已启用 ClawBot 插件 → 下一步：授权 */
    fun onPluginEnabled() {
        val current = _uiState.value
        if (current is IlinkUiState.Setup && current.step == SetupStep.ENABLE_PLUGIN) {
            FileLogger.i(TAG, "onPluginEnabled → AUTHORIZE")
            _uiState.value = current.copy(step = SetupStep.AUTHORIZE)
            startAuthorization()
        } else {
            FileLogger.w(TAG, "onPluginEnabled 忽略: state=$current")
        }
    }

    /** 开始授权流程 */
    private fun startAuthorization() {
        // 启动前台 Service 保活，避免 MIUI 切断后台网络
        startAuthKeepAliveService()

        viewModelScope.launch {
            try {
                FileLogger.i(TAG, "startAuthorization: 请求授权二维码")
                val qrResult = withContext(Dispatchers.IO) {
                    ilinkClient.getBotQrCode()
                }
                qrResult.onSuccess { qr ->
                    val current = _uiState.value as? IlinkUiState.Setup ?: return@launch
                    if (qr.qrcode == null || qr.url == null) {
                        FileLogger.e(TAG, "授权二维码返回数据不完整: qrcode=${qr.qrcode}, url=${qr.url}")
                        stopAuthPollingService()
                        _uiState.value = IlinkUiState.Error("获取授权链接失败: 返回数据不完整")
                        return@launch
                    }
                    FileLogger.i(TAG, "授权二维码获取成功: url=${qr.url.take(100)}")
                    _uiState.value = current.copy(qrCodeUrl = qr.url)
                    pollQrCodeStatus(qr.qrcode)
                }.onFailure { e ->
                    stopAuthPollingService()
                    val msg = e.localizedMessage ?: e::class.simpleName ?: "未知错误"
                    FileLogger.e(TAG, "获取授权二维码失败", e)
                    _uiState.value = IlinkUiState.Error("获取授权链接失败: $msg")
                }
            } catch (e: Exception) {
                stopAuthPollingService()
                val msg = e.localizedMessage ?: e::class.simpleName ?: "未知错误"
                FileLogger.e(TAG, "授权流程异常", e)
                _uiState.value = IlinkUiState.Error("授权失败: $msg")
            }
        }
    }

    private fun startAuthKeepAliveService() {
        try {
            val intent = Intent(context, IlinkBridgeService::class.java).apply {
                action = IlinkBridgeService.ACTION_POLL_AUTH
            }
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            FileLogger.w(TAG, "启动授权保活 Service 失败: ${e.localizedMessage}")
        }
    }

    private fun stopAuthPollingService() {
        try {
            val intent = Intent(context, IlinkBridgeService::class.java).apply {
                action = IlinkBridgeService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            FileLogger.w(TAG, "停止授权 Service 失败: ${e.localizedMessage}")
        }
    }

    private suspend fun pollQrCodeStatus(qrcodeRaw: String) {
        FileLogger.i(TAG, "pollQrCodeStatus: 开始轮询授权状态")
        var retries = 0
        var consecutiveFailures = 0
        while (retries < 120) {
            delay(1000)
            val statusResult = withContext(Dispatchers.IO) {
                ilinkClient.getQrCodeStatus(qrcodeRaw)
            }
            statusResult.onSuccess { status ->
                consecutiveFailures = 0
                FileLogger.i(TAG, "pollQrCodeStatus retry=$retries: status=${status.status}")
                when (status.status) {
                    "confirmed" -> {
                        status.botToken?.let { authManager.saveToken(it) }
                        FileLogger.i(TAG, "授权确认成功!")
                        stopAuthPollingService()
                        val current = _uiState.value as? IlinkUiState.Setup ?: return
                        _uiState.value = current.copy(step = SetupStep.COMPLETE)
                        return
                    }
                    "wait", "scanned" -> { /* 继续 */ }
                    else -> {
                        FileLogger.w(TAG, "授权状态异常: ${status.status}")
                        stopAuthPollingService()
                        _uiState.value = IlinkUiState.Error("授权失败: ${status.status}")
                        return
                    }
                }
            }.onFailure { e ->
                consecutiveFailures++
                FileLogger.e(TAG, "轮询授权状态失败 ($consecutiveFailures 次连续): ${e.localizedMessage ?: e::class.simpleName}", e)
                if (consecutiveFailures >= 5) {
                    FileLogger.w(TAG, "连续 5 次轮询失败，停止重试")
                    stopAuthPollingService()
                    _uiState.value = IlinkUiState.Error("网络连接失败，请确认网络正常后重试")
                    return
                }
            }
            retries++
        }
        FileLogger.w(TAG, "pollQrCodeStatus 超时")
        stopAuthPollingService()
        _uiState.value = IlinkUiState.Error("授权超时，请重试")
    }

    /** 授权完成，启动桥接 */
    fun onSetupComplete() {
        FileLogger.i(TAG, "onSetupComplete → startBridge")
        startBridge()
    }

    /** 启动桥接 Service */
    fun startBridge() {
        val name = (_uiState.value as? IlinkUiState.Connected)?.skillName
            ?: (_uiState.value as? IlinkUiState.Stopped)?.skillName
            ?: runBlockingSafe { getCurrentSkill()?.name } ?: "未选择"
        FileLogger.i(TAG, "startBridge: skill=$name")
        _uiState.value = IlinkUiState.Connected(skillName = name)
        val intent = Intent(context, IlinkBridgeService::class.java).apply {
            action = IlinkBridgeService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopBridge() {
        val currentName = when (val s = _uiState.value) {
            is IlinkUiState.Connected -> s.skillName
            else -> runBlockingSafe { getCurrentSkill()?.name } ?: "未选择"
        }
        FileLogger.i(TAG, "stopBridge: skill=$currentName")
        _uiState.value = IlinkUiState.Stopped(skillName = currentName)
        val intent = Intent(context, IlinkBridgeService::class.java).apply {
            action = IlinkBridgeService.ACTION_STOP
        }
        context.startService(intent)
    }

    /** 切换当前角色 */
    fun switchSkill(skillId: String) {
        viewModelScope.launch {
            FileLogger.i(TAG, "switchSkill: id=$skillId")
            authManager.setIlinkSkillId(skillId)
            val skill = skillRepository.getSkillById(skillId)
            val name = skill?.name ?: "未选择"
            _uiState.update { state ->
                when (state) {
                    is IlinkUiState.Connected -> state.copy(skillName = name)
                    is IlinkUiState.Stopped -> state.copy(skillName = name)
                    is IlinkUiState.Setup -> state.copy(currentSkill = skill)
                    else -> state
                }
            }
        }
    }

    /** 重置到未授权状态 */
    fun resetAuth() {
        viewModelScope.launch {
            FileLogger.i(TAG, "resetAuth: 清除 Token")
            authManager.clearToken()
            stopBridge()
            _uiState.value = IlinkUiState.Setup(
                step = SetupStep.ENABLE_PLUGIN,
                currentSkill = getCurrentSkill()
            )
        }
    }

    /** 测试发送今日定时消息 */
    fun testSendSchedule() {
        FileLogger.i(TAG, "testSendSchedule: 触发测试发送")
        val intent = Intent(context, IlinkBridgeService::class.java).apply {
            action = IlinkBridgeService.ACTION_TEST_SCHEDULE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /** 重新连接 */
    fun reconnect() {
        viewModelScope.launch {
            val authed = authManager.isAuthenticated()
            FileLogger.i(TAG, "reconnect: authed=$authed")
            if (authed) {
                startBridge()
            } else {
                _uiState.value = IlinkUiState.Setup(
                    step = SetupStep.ENABLE_PLUGIN,
                    currentSkill = getCurrentSkill()
                )
            }
        }
    }

    private suspend fun getCurrentSkill(): Skill? {
        // ClawBot 独立角色，未设置则回退聊天页选中角色
        val id = authManager.getIlinkSkillId() ?: skillRepository.getSelectedSkillId()
        val skill = skillRepository.getSkillById(id)
        FileLogger.i(TAG, "getCurrentSkill: id=$id → ${skill?.name}")
        return skill
    }
}

/** viewModelScope 内安全执行 blocking 代码 */
private fun <T> runBlockingSafe(block: suspend () -> T): T? {
    return try {
        kotlinx.coroutines.runBlocking { block() }
    } catch (_: Exception) {
        null
    }
}
