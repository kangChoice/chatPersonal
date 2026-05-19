"use client";

import { useState, useEffect, useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import { useSectionTransition } from "@/components/animations/sectionTransition";

interface MultiChatMessage {
  name: string;
  emoji: string;
  color: string;
  content: string;
}

const participants = [
  { name: "PM", emoji: "📋", color: "#88E2CE" },
  { name: "设计", emoji: "🎨", color: "#FFAEC9" },
  { name: "开发", emoji: "⚙️", color: "#5B9DFF" },
];

const demoMessages: MultiChatMessage[] = [
  {
    name: "PM",
    emoji: "📋",
    color: "#88E2CE",
    content: "用户反馈登录流程太复杂了，注册转化率掉了 15%",
  },
  {
    name: "设计",
    emoji: "🎨",
    color: "#FFAEC9",
    content: "我建议简化成三步：手机号 → 验证码 → 完成。去掉密码设置环节",
  },
  {
    name: "开发",
    emoji: "⚙️",
    color: "#5B9DFF",
    content: "技术上可行，短信验证码接入已经做好了，给我两天时间改",
  },
  {
    name: "PM",
    emoji: "📋",
    color: "#88E2CE",
    content: "好，那先出个简化方案，下周上线 A/B 测试",
  },
];

export default function MultiChatDemo() {
  const [visibleMessages, setVisibleMessages] = useState(0);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  useEffect(() => {
    const timers: NodeJS.Timeout[] = [];
    demoMessages.forEach((_, i) => {
      const timer = setTimeout(() => setVisibleMessages(i + 1), 1500 + i * 1200);
      timers.push(timer);
    });
    return () => timers.forEach(clearTimeout);
  }, []);

  return (
    <section
      id="multichat"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">群聊模式</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            多角色同场，思维碰撞。
          </p>
          <p className="text-text-tertiary leading-relaxed">
            让多个 AI 角色同时参与对话，每个角色都有独立的性格和知识背景。
            产品讨论、头脑风暴、创意碰撞 —— 像真实团队一样协作。
          </p>
        </div>

        {/* Right: MultiChat mockup */}
        <div className="glass-card p-4 w-full max-w-sm mx-auto">
          {/* Participants bar */}
          <div className="flex items-center gap-3 mb-4 pb-3 border-b border-white/20">
            {participants.map((p) => (
              <div key={p.name} className="flex items-center gap-2">
                <div
                  className="w-7 h-7 rounded-full flex items-center justify-center text-xs"
                  style={{ background: `${p.color}33` }}
                >
                  {p.emoji}
                </div>
                <span className="text-xs font-medium" style={{ color: p.color }}>
                  {p.name}
                </span>
              </div>
            ))}
            <span className="text-[10px] text-text-tertiary ml-auto">3 人在线</span>
          </div>

          {/* Messages */}
          <div className="space-y-3 min-h-[280px]">
            {demoMessages.slice(0, visibleMessages).map((msg, i) => (
              <motion.div
                key={i}
                className="flex items-start gap-2"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.4, ease: "easeOut" }}
              >
                <div
                  className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] mt-1 shrink-0"
                  style={{ background: `${msg.color}33` }}
                >
                  {msg.emoji}
                </div>
                <div
                  className="rounded-[16px] rounded-tl-[4px] px-3.5 py-2 text-sm leading-relaxed max-w-[85%]"
                  style={{
                    background: `${msg.color}18`,
                    border: `0.5px solid ${msg.color}30`,
                    color: "#2D3748",
                  }}
                >
                  <span className="text-xs font-semibold" style={{ color: msg.color }}>
                    {msg.name}
                  </span>
                  <p className="mt-0.5">{msg.content}</p>
                </div>
              </motion.div>
            ))}
            {visibleMessages === 0 && (
              <div className="flex items-center justify-center h-[250px] text-text-tertiary text-sm">
                正在邀请角色加入群聊…
              </div>
            )}
          </div>

          {/* Input bar */}
          <div className="mt-4 pt-3 border-t border-white/20">
            <div
              className="flex items-center gap-2 rounded-full px-4 py-2"
              style={{
                background: "rgba(255,255,255,0.7)",
                border: "0.5px solid rgba(255,255,255,0.5)",
              }}
            >
              <span className="text-text-tertiary text-sm flex-1">@所有人 发送消息…</span>
              <div className="w-7 h-7 rounded-full glass-nav-btn flex items-center justify-center">
                <svg
                  className="w-3.5 h-3.5 text-white"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M5 12h14M12 5l7 7-7 7"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
