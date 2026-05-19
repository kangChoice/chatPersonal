"use client";

import { useState, useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import GlassCard from "@/components/ui/GlassCard";
import { useSectionTransition } from "@/components/animations/sectionTransition";

const providers = [
  { name: "OpenAI", icon: "🔵", desc: "GPT-4o / GPT-4o-mini", color: "#5B9DFF" },
  { name: "Anthropic", icon: "🟣", desc: "Claude Sonnet / Opus", color: "#FFAEC9" },
  { name: "DeepSeek", icon: "🟢", desc: "DeepSeek V3 / R1", color: "#88E2CE" },
];

const backgrounds = [
  { label: "默认", color: "#F9F9FB" },
  { label: "极简", color: "#FFFFFF" },
  { label: "暮色", color: "#1a1a2e" },
  { label: "森林", color: "#2d5a27" },
];

export default function SettingsDemo() {
  const [isDark, setIsDark] = useState(false);
  const [selectedBg, setSelectedBg] = useState(0);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  return (
    <section
      id="settings"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">设置页面</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            灵活配置，随心定制。
          </p>
          <p className="text-text-tertiary leading-relaxed">
            多模型支持、亮暗主题切换、自定义聊天背景…
            每一个细节都可以按照你的喜好调整。
          </p>
        </div>

        {/* Right: Settings mockup */}
        <div
          className="glass-card p-5 w-full max-w-sm mx-auto"
          style={{
            background: isDark
              ? "rgba(30,30,30,0.85)"
              : "rgba(255,255,255,0.7)",
          }}
        >
          {/* Model config */}
          <h3 className="text-xs font-semibold text-text-tertiary mb-3 tracking-wider">
            模型配置
          </h3>
          <div className="space-y-2 mb-6">
            {providers.map((p, i) => (
              <motion.div
                key={p.name}
                className="stagger-item flex items-center gap-3 rounded-xl px-3.5 py-2.5"
                style={{
                  background: isDark ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.5)",
                }}
                whileHover={{ x: 3 }}
              >
                <span className="text-lg">{p.icon}</span>
                <div className="flex-1">
                  <span
                    className="text-sm font-medium"
                    style={{
                      color: isDark ? "#F0EBF0" : "#111318",
                    }}
                  >
                    {p.name}
                  </span>
                  <span className="text-[10px] text-text-tertiary ml-2">
                    {p.desc}
                  </span>
                </div>
                <span
                  className="w-2 h-2 rounded-full"
                  style={{ background: p.color }}
                />
              </motion.div>
            ))}
          </div>

          {/* Theme toggle */}
          <h3 className="text-xs font-semibold text-text-tertiary mb-3 tracking-wider">
            显示模式
          </h3>
          <div
            className="stagger-item flex items-center justify-between rounded-xl px-4 py-3 mb-6"
            style={{
              background: isDark ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.5)",
            }}
          >
            <span
              className="text-sm"
              style={{
                color: isDark ? "#F0EBF0" : "#111318",
              }}
            >
              {isDark ? "🌙 深色模式" : "☀️ 浅色模式"}
            </span>
            <button
              onClick={() => setIsDark(!isDark)}
              className="relative w-11 h-6 rounded-full transition-colors duration-300"
              style={{
                background: isDark ? "#5B9DFF" : "#CBD5E0",
              }}
            >
              <motion.div
                className="absolute top-0.5 w-5 h-5 rounded-full bg-white shadow"
                animate={{ x: isDark ? 22 : 2 }}
                transition={{ type: "spring", stiffness: 500, damping: 30 }}
              />
            </button>
          </div>

          {/* Background picker */}
          <h3 className="text-xs font-semibold text-text-tertiary mb-3 tracking-wider">
            聊天背景
          </h3>
          <div className="flex gap-2.5 stagger-item">
            {backgrounds.map((bg, i) => (
              <button
                key={bg.label}
                onClick={() => setSelectedBg(i)}
                className="flex flex-col items-center gap-1.5"
              >
                <div
                  className="w-10 h-10 rounded-xl transition-all duration-300"
                  style={{
                    background: bg.color,
                    border:
                      i === selectedBg
                        ? "2px solid #5B9DFF"
                        : "0.5px solid rgba(255,255,255,0.5)",
                    boxShadow:
                      i === selectedBg
                        ? "0 0 0 3px rgba(91,157,255,0.2)"
                        : "none",
                  }}
                />
                <span className="text-[10px] text-text-tertiary">
                  {bg.label}
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
