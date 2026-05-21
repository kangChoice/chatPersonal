"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import { useSectionTransition } from "@/components/animations/sectionTransition";

type Step = "intro" | "authorize" | "connected";

const demoSkills = [
  { name: "学习助手", emoji: "📚", color: "#88E2CE" },
  { name: "编程导师", emoji: "💻", color: "#5B9DFF" },
  { name: "心理树洞", emoji: "🌿", color: "#FFAEC9" },
];

const demoConversation = [
  { role: "friend", name: "小明", text: "在吗？帮我查一下明天天气" },
  { role: "bot", text: "明天北京多云转晴，18°C~26°C，适合出门哦~" },
  { role: "friend", name: "小红", text: "能不能帮我写一段工作汇报" },
  {
    role: "bot",
    text: "当然可以！本周完成了XX项目需求评审和接口联调，下周计划推进上线流程。需要我展开写吗？",
  },
];

export default function IlinkDemo() {
  const [step, setStep] = useState<Step>("intro");
  const [selectedSkill, setSelectedSkill] = useState(0);
  const [visibleMessages, setVisibleMessages] = useState(0);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  const handleStart = () => {
    setStep("authorize");
    setTimeout(() => setStep("connected"), 2500);
  };

  const startMessageAnimation = useCallback(() => {
    setVisibleMessages(0);
    const timers: NodeJS.Timeout[] = [];
    demoConversation.forEach((_, i) => {
      timers.push(setTimeout(() => setVisibleMessages(i + 1), i * 1200));
    });
    return timers;
  }, []);

  useEffect(() => {
    if (step !== "connected") return;
    const timers = startMessageAnimation();
    return () => timers.forEach(clearTimeout);
  }, [step, startMessageAnimation]);

  return (
    <section
      id="ilink"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div
        className="glow-orb"
        style={{
          width: 400,
          height: 400,
          background: "radial-gradient(circle, rgba(136,226,206,0.1))",
          left: "50%",
          top: "50%",
          transform: "translate(-50%, -50%)",
        }}
      />

      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center relative z-10">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">微信接入</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            让你的 AI 角色"住进"微信，好友直接对话。
          </p>
          <p className="text-text-tertiary leading-relaxed mb-6">
            通过微信官方 ClawBot 插件，将你的 AI 角色接入微信。
            好友发消息 → AI 自动回复，支持角色切换、定时消息，
            完全后台运行，无需保持 App 前台。
          </p>

          <div className="flex flex-wrap gap-3">
            {["微信 ClawBot", "扫码授权", "角色切换", "定时消息"].map(
              (tag, i) => (
                <span
                  key={tag}
                  className="stagger-item text-xs px-3 py-1.5 rounded-full glass"
                >
                  <span
                    className="w-1.5 h-1.5 rounded-full inline-block mr-1.5"
                    style={{
                      background: ["#88E2CE", "#5B9DFF", "#FFAEC9", "#FFD166"][i],
                    }}
                  />
                  {tag}
                </span>
              )
            )}
          </div>
        </div>

        {/* Right: Mockup */}
        <div className="glass-card p-5 w-full max-w-sm mx-auto min-h-[440px]">
          <AnimatePresence mode="wait">
            {step === "intro" && (
              <motion.div
                key="intro"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                {/* Header */}
                <div className="flex items-center gap-3 mb-4">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center text-lg"
                    style={{ background: "rgba(136,226,206,0.2)" }}
                  >
                    💬
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-text-primary">
                      微信 ClawBot
                    </h3>
                    <p className="text-[10px] text-text-tertiary">
                      官方插件 · 安全可靠
                    </p>
                  </div>
                </div>

                {/* Steps */}
                <div className="flex items-center gap-3 mb-6">
                  {["启用插件", "扫码授权", "开始使用"].map((label, i) => (
                    <div
                      key={label}
                      className="flex items-center gap-1.5"
                    >
                      <div
                        className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold text-white"
                        style={{
                          background: i === 0 ? "#88E2CE" : "rgba(0,0,0,0.15)",
                        }}
                      >
                        {i + 1}
                      </div>
                      <span className="text-[10px] text-text-tertiary">
                        {label}
                      </span>
                      {i < 2 && (
                        <div className="w-4 h-px bg-white/20" />
                      )}
                    </div>
                  ))}
                </div>

                {/* Skill selector */}
                <h4 className="text-xs font-semibold text-text-secondary mb-2">
                  选择回复角色
                </h4>
                <div className="flex gap-2 mb-5">
                  {demoSkills.map((skill, i) => (
                    <button
                      key={skill.name}
                      onClick={() => setSelectedSkill(i)}
                      className="flex-1 flex flex-col items-center gap-1 rounded-xl py-2.5 transition-all duration-300"
                      style={{
                        background:
                          i === selectedSkill
                            ? `${skill.color}18`
                            : "rgba(255,255,255,0.4)",
                        border:
                          i === selectedSkill
                            ? `1px solid ${skill.color}`
                            : "0.5px solid rgba(255,255,255,0.3)",
                      }}
                    >
                      <span className="text-lg">{skill.emoji}</span>
                      <span
                        className="text-[10px] font-medium"
                        style={{
                          color:
                            i === selectedSkill
                              ? skill.color
                              : "var(--color-text-tertiary)",
                        }}
                      >
                        {skill.name}
                      </span>
                    </button>
                  ))}
                </div>

                {/* Start button */}
                <button
                  onClick={handleStart}
                  className="w-full py-3 rounded-xl text-sm font-semibold text-white transition-all duration-300 hover:opacity-90"
                  style={{
                    background: "linear-gradient(135deg, #88E2CE, #5B9DFF)",
                  }}
                >
                  开始接入
                </button>
              </motion.div>
            )}

            {step === "authorize" && (
              <motion.div
                key="authorize"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex flex-col items-center justify-center min-h-[380px]"
              >
                {/* QR Code placeholder */}
                <motion.div
                  className="w-48 h-48 rounded-2xl mb-4 flex items-center justify-center"
                  style={{
                    background: "rgba(255,255,255,0.5)",
                    border: "1px solid rgba(0,0,0,0.08)",
                  }}
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ duration: 0.5 }}
                >
                  <div className="text-center">
                    <svg
                      className="w-24 h-24 mx-auto mb-2 text-[#88E2CE]"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M12 4v1m6 11h2m-6 0h-2.48a2.5 2.5 0 00-4.9 0H4m16 0h1.5M4 15h1.5M4 11h16M6.5 4v3m11-3v3M4 8h16a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2v-8a2 2 0 012-2z"
                      />
                    </svg>
                    <span className="text-xs text-text-tertiary">
                      微信扫码授权
                    </span>
                  </div>
                </motion.div>

                <motion.div
                  className="flex items-center gap-2 text-sm text-[#88E2CE]"
                  animate={{ opacity: [1, 0.5, 1] }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                >
                  <span className="w-2 h-2 rounded-full bg-[#88E2CE]" />
                  等待扫码确认...
                </motion.div>
              </motion.div>
            )}

            {step === "connected" && (
              <motion.div
                key="connected"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                {/* Connected header */}
                <div className="flex items-center justify-between mb-4 pb-3 border-b border-white/20">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-[#88E2CE] animate-pulse" />
                    <span className="text-sm font-semibold text-text-primary">
                      已连接
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-lg">{demoSkills[selectedSkill].emoji}</span>
                    <span className="text-xs text-text-secondary">
                      {demoSkills[selectedSkill].name}
                    </span>
                  </div>
                </div>

                {/* Conversation */}
                <div className="space-y-2.5 min-h-[280px] mb-4">
                  {demoConversation.slice(0, visibleMessages).map((msg, i) => (
                    <motion.div
                      key={i}
                      className={`flex ${
                        msg.role === "bot" ? "justify-start" : "justify-end"
                      }`}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3 }}
                    >
                      <div className="max-w-[82%]">
                        {msg.role === "bot" ? (
                          <div
                            className="rounded-2xl rounded-tl-sm px-3.5 py-2.5 text-xs leading-relaxed"
                            style={{
                              background: `${demoSkills[selectedSkill].color}18`,
                              border: `0.5px solid ${demoSkills[selectedSkill].color}30`,
                              color: "var(--color-text-primary)",
                            }}
                          >
                            <span className="text-[10px] mr-1.5 opacity-60">
                              {demoSkills[selectedSkill].emoji}
                            </span>
                            {msg.text}
                          </div>
                        ) : (
                          <div>
                            <span className="text-[10px] text-text-tertiary block mb-0.5 ml-1">
                              {"name" in msg ? msg.name : ""}
                            </span>
                            <div
                              className="rounded-2xl rounded-tr-sm px-3.5 py-2.5 text-xs leading-relaxed text-white"
                              style={{
                                background:
                                  "linear-gradient(135deg, #88E2CE, #5B9DFF)",
                              }}
                            >
                              {msg.text}
                            </div>
                          </div>
                        )}
                      </div>
                    </motion.div>
                  ))}

                  {visibleMessages === 0 && (
                    <div className="flex items-center justify-center h-[250px] text-text-tertiary text-xs">
                      等待消息中...
                    </div>
                  )}
                </div>

                {/* Bottom bar */}
                <div
                  className="rounded-xl px-3 py-2 flex items-center gap-2 text-[10px] text-text-tertiary"
                  style={{ background: "rgba(255,255,255,0.3)" }}
                >
                  <span>🕐 定时消息已启用</span>
                  <span>·</span>
                  <span>后台运行中</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </section>
  );
}
