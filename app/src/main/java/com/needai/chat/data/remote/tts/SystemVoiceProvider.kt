package com.needai.chat.data.remote.tts

import com.needai.chat.domain.model.SystemVoice

/**
 * 阿里云 CosyVoice 系统预置音色列表
 *
 * 数据来源：https://help.aliyun.com/zh/model-studio/cosyvoice-voice-list
 *
 * 注意：
 * - v3.5-flash / v3.5-plus 无系统预置音色，仅支持设计音色（声音克隆）
 * - v3-plus 仅标杆音色（龙安洋、龙安欢）支持
 * - 各模型音色不能混用，必须使用对应 voice 参数
 */
object SystemVoiceProvider {

    private val voiceMap: Map<String, List<SystemVoice>> = mapOf(
        "cosyvoice-v1" to listOf(
            SystemVoice("zhimi", "知米", "治愈系女声;中文", listOf("cosyvoice-v1")),
            SystemVoice("aicheng", "艾成", "磁性男声;中文", listOf("cosyvoice-v1")),
            SystemVoice("xiaoyun", "晓云", "温柔女声;中文", listOf("cosyvoice-v1")),
            SystemVoice("longwan", "龙婉", "细腻柔声女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longcheng", "龙橙", "智慧青年男;中文", listOf("cosyvoice-v1")),
            SystemVoice("longhua", "龙华", "元气甜美女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longxiaochun", "龙小淳", "知性积极女;中文、英文", listOf("cosyvoice-v1")),
            SystemVoice("longxiaoxia", "龙小夏", "沉稳权威女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longxiaocheng", "龙小诚", "磁性低音男;中文、英文", listOf("cosyvoice-v1")),
            SystemVoice("longxiaobai", "龙小白", "沉稳播报女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longlaotie", "龙老铁", "东北直率男;中文东北口音", listOf("cosyvoice-v1")),
            SystemVoice("longshu", "龙书", "沉稳青年男;中文", listOf("cosyvoice-v1")),
            SystemVoice("longshuo", "龙硕", "博才干练男;中文", listOf("cosyvoice-v1")),
            SystemVoice("longjing", "龙婧", "典型播音女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longmiao", "龙妙", "抑扬顿挫女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longyue", "龙悦", "温暖磁性女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longyuan", "龙媛", "温暖治愈女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longfei", "龙飞", "热血磁性男;中文", listOf("cosyvoice-v1")),
            SystemVoice("longjielidou", "龙杰力豆", "阳光顽皮男;中文、英文", listOf("cosyvoice-v1")),
            SystemVoice("longtong", "龙彤", "元气活泼女;中文", listOf("cosyvoice-v1")),
            SystemVoice("longxiang", "龙祥", "沉稳男声;中文", listOf("cosyvoice-v1")),
            SystemVoice("loongstella", "Stella", "飒爽利落女;中文、英文", listOf("cosyvoice-v1")),
            SystemVoice("loongbella", "Bella", "精准干练女;中文", listOf("cosyvoice-v1")),
        ),
        "cosyvoice-v2" to listOf(
            // 童声
            SystemVoice("longhuhu", "龙呼呼", "天真烂漫女童;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longjielidou_v2", "龙杰力豆", "阳光顽皮男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longling_v2", "龙铃", "稚气呆板女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longke_v2", "龙可", "懵懂乖乖女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxian_v2", "龙仙", "豪放可爱女;中文、英文", listOf("cosyvoice-v2")),
            // 社交陪伴
            SystemVoice("longanqin", "龙安亲", "亲和活泼女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanya", "龙安雅", "高雅气质女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanshuo", "龙安朔", "干净清爽男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longanling", "龙安灵", "思维灵动女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanzhi", "龙安智", "睿智轻熟男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanrou", "龙安柔", "温柔闺蜜女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longqiang_v2", "龙嫱", "浪漫风情女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longhan_v2", "龙寒", "温暖痴情男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxing_v2", "龙星", "温婉邻家女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longhua_v2", "龙华", "元气甜美女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longwan_v2", "龙婉", "积极知性女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longcheng_v2", "龙橙", "智慧青年男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longfeifei_v2", "龙菲菲", "甜美娇气女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxiaocheng_v2", "龙小诚", "磁性低音男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longzhe_v2", "龙哲", "呆板大暖男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyan_v2", "龙颜", "温暖春风女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longtian_v2", "龙天", "磁性理智男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longze_v2", "龙泽", "温暖元气男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longshao_v2", "龙邵", "积极向上男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longhao_v2", "龙浩", "多情忧郁男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("kabuleshen_v2", "龙深", "实力歌手男;中文、英文", listOf("cosyvoice-v2")),
            // 有声书
            SystemVoice("longyichen", "龙逸尘", "洒脱活力男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longwanjun", "龙婉君", "细腻柔声女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longlaobo", "龙老伯", "沧桑岁月爷;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longlaoyi", "龙老姨", "烟火从容阿姨;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longbaizhi", "龙白芷", "睿气旁白女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longsanshu", "龙三叔", "沉稳质感男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longxiu_v2", "龙修", "博才说书男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longmiao_v2", "龙妙", "抑扬顿挫女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyue_v2", "龙悦", "温暖磁性女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longnan_v2", "龙楠", "睿智青年男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyuan_v2", "龙媛", "温暖治愈女;中文、英文", listOf("cosyvoice-v2")),
            // 语音助手
            SystemVoice("longanli", "龙安莉", "利落从容女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanlang", "龙安朗", "清爽利落男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanwen", "龙安温", "优雅知性女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanyun", "龙安昀", "居家暖男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longyumi_v2", "YUMI", "正经青年女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxiaochun_v2", "龙小淳", "知性积极女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxiaoxia_v2", "龙小夏", "沉稳权威女;中文、英文", listOf("cosyvoice-v2")),
            // 客服
            SystemVoice("longyingmu", "龙应沐", "优雅知性女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longyingxun", "龙应询", "年轻青涩男;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longyingcui", "龙应催", "严肃催收男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyingda", "龙应答", "开朗高音女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyingjing", "龙应静", "低调冷静女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longyingyan", "龙应严", "义正严辞女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyingtian", "龙应甜", "温柔甜美女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyingbing", "龙应冰", "尖锐强势女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longyingtao", "龙应桃", "温柔淡定女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longyingling", "龙应聆", "温和共情女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            // 直播带货
            SystemVoice("longanran", "龙安燃", "活泼质感女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanxuan", "龙安宣", "经典直播女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanchong", "龙安冲", "激情推销男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longanping", "龙安萍", "高亢直播女;中文、英文", listOf("cosyvoice-v2")),
            // 电话销售
            SystemVoice("longyingxiao", "龙应笑", "清甜推销女;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            // 短视频配音
            SystemVoice("longjiqi", "龙机器", "呆萌机器人;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longhouge", "龙猴哥", "经典猴哥;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longjixin", "龙机心", "毒舌心机女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longanyue", "龙安粤", "欢脱粤语男;中文(粤语)、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longshange", "龙陕哥", "原味陕北男;中文(陕西话)、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longanmin", "龙安敏", "甜美闽南女;中文(闽南话)、英文", listOf("cosyvoice-v2")),
            SystemVoice("longdaiyu", "龙黛玉", "娇率才女音;中文、英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash")),
            SystemVoice("longgaoseng", "龙高僧", "得道高僧音;中文、英文", listOf("cosyvoice-v2")),
            // 消费电子-教育培训
            SystemVoice("longanpei", "龙安培", "青少年教师女;中文、英文", listOf("cosyvoice-v2")),
            // 消费电子-儿童陪伴
            SystemVoice("longwangwang", "龙汪汪", "台湾少年音;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longpaopao", "龙泡泡", "飞天泡泡音;中文、英文", listOf("cosyvoice-v2")),
            // 消费电子-儿童有声书
            SystemVoice("longshanshan", "龙闪闪", "戏剧化童声;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longniuniu", "龙牛牛", "阳光男童声;中文、英文", listOf("cosyvoice-v2")),
            // 诗词朗诵
            SystemVoice("longfei_v2", "龙飞", "热血磁性男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("libai_v2", "李白", "古代诗仙男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longjin_v2", "龙津", "优雅温润男;中文、英文", listOf("cosyvoice-v2")),
            // 新闻播报
            SystemVoice("longshu_v2", "龙书", "沉稳青年男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("loongbella_v2", "Bella2.0", "精准干练女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longshuo_v2", "龙硕", "博才干练男;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longxiaobai_v2", "龙小白", "沉稳播报女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("longjing_v2", "龙婧", "典型播音女;中文、英文", listOf("cosyvoice-v2")),
            SystemVoice("loongstella_v2", "loongstella", "飒爽利落女;中文、英文", listOf("cosyvoice-v2")),
            // 出海营销（仅北京地域）
            SystemVoice("loongyuuna_v2", "loongyuuna", "元气霓虹女;日语", listOf("cosyvoice-v2")),
            SystemVoice("loongyuuma_v2", "loongyuuma", "干练霓虹男;日语", listOf("cosyvoice-v2")),
            SystemVoice("loongjihun_v2", "loongjihun", "阳光韩国男;韩语", listOf("cosyvoice-v2")),
            SystemVoice("loongeva_v2", "loongeva", "知性英文女;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongbrian_v2", "loongbrian", "沉稳英文男;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongluna_v2", "loongluna", "英式英文女;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongluca_v2", "loongluca", "英式英文男;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongemily_v2", "loongemily", "英式英文女;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongeric_v2", "loongeric", "英式英文男;英式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongabby_v2", "loongabby", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongannie_v2", "loongannie", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongandy_v2", "loongandy", "美式英文男;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongava_v2", "loongava", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongbeth_v2", "loongbeth", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongbetty_v2", "loongbetty", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongcindy_v2", "loongcindy", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongcally_v2", "loongcally", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongdavid_v2", "loongdavid", "美式英文男;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongdonna_v2", "loongdonna", "美式英文女;美式英文", listOf("cosyvoice-v2")),
            SystemVoice("loongkyong_v2", "loongkyong", "韩语女;韩语", listOf("cosyvoice-v2")),
            SystemVoice("loongtomoka_v2", "loongtomoka", "日语女;日语", listOf("cosyvoice-v2")),
            SystemVoice("loongtomoya_v2", "loongtomoya", "日语男;日语", listOf("cosyvoice-v2")),
        ),
        "cosyvoice-v3-flash" to listOf(
            // v3-flash 仅有两个系统标杆音色，其余为设计音色
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中文(普通话)、英文", listOf("cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("longanhuan", "龙安欢", "欢脱元气女;中文(普通话)、英文", listOf("cosyvoice-v3-flash", "cosyvoice-v3-plus")),
        ),
        "cosyvoice-v3-plus" to listOf(
            // v3-plus 仅标杆音色
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中文(普通话)、英文", listOf("cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("longanhuan", "龙安欢", "欢脱元气女;中文(普通话)、英文", listOf("cosyvoice-v3-flash", "cosyvoice-v3-plus")),
        ),
    )

    fun getVoices(model: String): List<SystemVoice> {
        if (model.startsWith("cosyvoice-v3.5")) {
            // v3.5 系列无系统预置音色，仅支持设计音色
            return emptyList()
        }
        return voiceMap.entries
            .firstOrNull { model.startsWith(it.key) }
            ?.value ?: emptyList()
    }

    fun hasSystemVoices(model: String): Boolean {
        if (model.startsWith("cosyvoice-v3.5")) {
            return false
        }
        return voiceMap.entries.any { model.startsWith(it.key) }
    }

    /**
     * 判断 voiceId 是否为系统预置音色，并返回推荐使用的模型。
     * 系统音色统一使用 cosyvoice-v3-flash 模型输出。
     * 返回 null 表示非系统音色（自定义设计音色）。
     */
    fun getModelForVoice(voiceId: String): String? {
        return if (voiceMap.values.any { voices -> voices.any { it.voiceId == voiceId } })
            "cosyvoice-v3-flash" else null
    }

    data class ParsedVoiceItem(
        val voiceId: String,
        val displayName: String,
        val description: String,
        val language: String
    )

    fun parseFromRaw(raw: String): ParsedVoiceItem? {
        val parts = raw.split("-", limit = 2)
        if (parts.size != 2) return null
        val voiceId = parts[0]
        val detailParts = parts[1].split(";")
        return ParsedVoiceItem(
            voiceId = voiceId,
            displayName = detailParts.getOrElse(0) { voiceId },
            description = detailParts.getOrElse(1) { "" },
            language = detailParts.getOrElse(2) { "" }
        )
    }
}
