"use client";

import { useState, useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import Waveform from "@/components/ui/Waveform";
import { useSectionTransition } from "@/components/animations/sectionTransition";

interface Voice {
  name: string;
  boundSkill: string;
  status: "available" | "deploying";
  color: string;
}

const demoVoices: Voice[] = [
  { name: "温柔知性", boundSkill: "心理树洞", status: "available", color: "#88E2CE" },
  { name: "活力少年", boundSkill: "编程教练", status: "available", color: "#5B9DFF" },
  { name: "知性学姐", boundSkill: "学习导师", status: "deploying", color: "#FFAEC9" },
];

export default function VoiceDemo() {
  const [playingIndex, setPlayingIndex] = useState<number | null>(null);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  const togglePlay = (i: number) => {
    setPlayingIndex(playingIndex === i ? null : i);
  };

  return (
    <section
      id="voice"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">音色管理</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            让 AI 拥有你想要的声音。
          </p>
          <p className="text-text-tertiary leading-relaxed mb-6">
            200+ 系统预置音色 + 声音克隆，从温柔知性到活力治愈。
            每个角色都可以绑定专属音色，让对话更有温度。
          </p>
          <div className="stagger-item inline-flex items-center gap-2 glass px-4 py-2 rounded-full text-sm text-text-secondary">
            <span className="w-2 h-2 rounded-full bg-[#88E2CE] animate-pulse" />
            200+ 系统音色 · 支持声音克隆
          </div>
        </div>

        {/* Right: Voice cards */}
        <div className="space-y-4 w-full max-w-sm mx-auto">
          {demoVoices.map((voice, i) => (
            <motion.div
              key={voice.name}
              className="glass-card p-4 flex items-center gap-4 stagger-item"
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: i * 0.15 }}
              whileHover={{ x: 4, transition: { duration: 0.2 } }}
            >
              {/* Play button */}
              <button
                onClick={() => togglePlay(i)}
                className="w-10 h-10 rounded-full shrink-0 flex items-center justify-center transition-all duration-300"
                style={{
                  background: playingIndex === i ? `${voice.color}33` : "rgba(255,255,255,0.7)",
                  border: `0.5px solid ${playingIndex === i ? voice.color : "rgba(255,255,255,0.5)"}`,
                }}
              >
                {playingIndex === i ? (
                  <Waveform isPlaying />
                ) : (
                  <svg className="w-4 h-4" style={{ color: voice.color }} fill="currentColor" viewBox="0 0 24 24">
                    <path d="M8 5v14l11-7z" />
                  </svg>
                )}
              </button>

              {/* Info */}
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-text-primary">{voice.name}</h3>
                <p className="text-xs text-text-tertiary">绑定：{voice.boundSkill}</p>
              </div>

              {/* Status */}
              <span
                className="text-[10px] px-2 py-1 rounded-full flex items-center gap-1"
                style={{
                  background: voice.status === "available" ? `${voice.color}22` : "rgba(91,157,255,0.15)",
                  color: voice.status === "available" ? voice.color : "#5B9DFF",
                }}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    voice.status === "available" ? "bg-[#88E2CE]" : "bg-[#5B9DFF] animate-pulse"
                  }`}
                />
                {voice.status === "available" ? "可用" : "部署中"}
              </span>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
