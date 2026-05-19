"use client";

import { useState, useEffect, useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import MessageBubble from "@/components/ui/MessageBubble";
import { useSectionTransition } from "@/components/animations/sectionTransition";

const demoMessages = [
  { role: "user" as const, content: "帮我解释一下量子计算是什么？" },
  {
    role: "ai" as const,
    content:
      "量子计算是一种利用量子力学原理（如叠加和纠缠）来进行信息处理的新型计算范式。",
  },
  { role: "user" as const, content: "那它和传统计算有什么区别呢？" },
];

const streamingText =
  "传统计算机用 0 或 1 的比特，而量子计算机用量子比特（qubit）。一个量子比特可以同时是 0 和 1（叠加态），再加上量子纠缠，让量子计算机在处理某些特定问题时拥有指数级的算力优势。简单说：传统计算机像开关（开/关），量子计算机像调光器（同时亮和暗的混合）。";

export default function ChatDemo() {
  const [visibleCount, setVisibleCount] = useState(0);
  const [streamingContent, setStreamingContent] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  useEffect(() => {
    const timer1 = setTimeout(() => setVisibleCount(1), 500);
    const timer2 = setTimeout(() => setVisibleCount(2), 1500);
    const timer3 = setTimeout(() => setVisibleCount(3), 2500);
    const timer4 = setTimeout(() => setIsStreaming(true), 3500);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
      clearTimeout(timer4);
    };
  }, []);

  useEffect(() => {
    if (!isStreaming) return;
    let i = 0;
    const interval = setInterval(() => {
      if (i < streamingText.length) {
        setStreamingContent(streamingText.slice(0, i + 1));
        i++;
      } else {
        clearInterval(interval);
      }
    }, 25);
    return () => clearInterval(interval);
  }, [isStreaming]);

  return (
    <section
      id="chat"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">智能聊天</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            流式对话，毫秒级响应。
          </p>
          <p className="text-text-tertiary leading-relaxed">
            支持 OpenAI、Anthropic 等多协议模型接入。
            消息实时流式输出，所见即所得。
            用户气泡薄荷蓝渐变，AI 气泡毛玻璃质感，
            每一处细节都精心打磨。
          </p>
        </div>

        {/* Right: Chat mockup */}
        <div className="glass-card p-4 w-full max-w-sm mx-auto">
          {/* Chat header */}
          <div className="flex items-center gap-3 mb-4 pb-3 border-b border-white/20">
            <div className="w-8 h-8 rounded-full glass flex items-center justify-center text-sm">
              🤖
            </div>
            <div>
              <p className="text-sm font-medium text-text-primary">
                AI 助手
              </p>
              <p className="text-[10px] text-text-tertiary">在线 · 即刻响应</p>
            </div>
          </div>

          {/* Messages */}
          <div className="space-y-3 min-h-[300px]">
            {demoMessages.slice(0, visibleCount).map((msg, i) => (
              <MessageBubble
                key={i}
                role={msg.role}
                content={msg.content}
                delay={i * 0.1}
              />
            ))}
            {isStreaming && (
              <div className="flex justify-start">
                <div className="w-7 h-7 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-xs mr-2 mt-1 shrink-0 border border-white/10">
                  🤖
                </div>
                <div
                  className="rounded-[20px] rounded-tl-[4px] px-4 py-2.5 text-sm leading-6 text-white/90 max-w-[80%]"
                  style={{
                    background: "rgba(0,0,0,0.45)",
                    border: "0.5px solid rgba(255,255,255,0.1)",
                  }}
                >
                  {streamingContent}
                  <span className="inline-block w-[2px] h-4 bg-white/70 ml-0.5 animate-pulse" />
                </div>
              </div>
            )}
            {visibleCount === 0 && (
              <div className="flex items-center justify-center h-[250px] text-text-tertiary text-sm">
                等待开始对话…
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
              <span className="text-text-tertiary text-sm flex-1">
                输入消息…
              </span>
              <div className="w-7 h-7 rounded-full bg-gradient-to-r from-[#88E2CE] to-[#5B9DFF] flex items-center justify-center">
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
