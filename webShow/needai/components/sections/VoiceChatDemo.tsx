"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import Waveform from "@/components/ui/Waveform";
import { useSectionTransition } from "@/components/animations/sectionTransition";

const demoSkills = [
  { name: "学习助手", emoji: "📚", color: "#88E2CE" },
  { name: "编程导师", emoji: "💻", color: "#5B9DFF" },
  { name: "心理树洞", emoji: "🌿", color: "#FFAEC9" },
];

const demoConversation = [
  { role: "user", text: "帮我写一首关于秋天的诗" },
  {
    role: "ai",
    text: "秋风起，落叶黄，一壶清茶伴斜阳。\n雁南飞，思绪长，满目星河皆文章。",
  },
  { role: "user", text: "很有意境！再来一首简短一点的" },
  {
    role: "ai",
    text: "一片落叶知秋至，\n半盏微凉岁月知。",
  },
];

export default function VoiceChatDemo() {
  const [phase, setPhase] = useState<"select" | "calling">("select");
  const [selectedSkill, setSelectedSkill] = useState<number | null>(null);
  const [visibleMessages, setVisibleMessages] = useState(0);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [partialText, setPartialText] = useState("");
  const [amplitude, setAmplitude] = useState(0);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  // Simulate ASR partial text when in call
  useEffect(() => {
    if (phase !== "calling") return;

    const asrTexts = ["嗯…", "我想想…", "有了，我比较喜欢那句", "很有意境！再来一首"];
    let i = 0;

    const partialInterval = setInterval(() => {
      if (i < asrTexts.length) {
        setPartialText(asrTexts[i]);
        setIsSpeaking(true);
        i++;
      } else {
        setPartialText("");
        setIsSpeaking(false);
        clearInterval(partialInterval);
      }
    }, 1800);

    return () => clearInterval(partialInterval);
  }, [phase]);

  // Simulate amplitude when speaking
  useEffect(() => {
    if (!isSpeaking) {
      setAmplitude(0);
      return;
    }

    const interval = setInterval(() => {
      setAmplitude(Math.floor(Math.random() * 180) + 40);
    }, 120);

    return () => clearInterval(interval);
  }, [isSpeaking]);

  // Simulate messages arriving
  useEffect(() => {
    if (phase !== "calling") {
      setVisibleMessages(0);
      return;
    }

    const timers: NodeJS.Timeout[] = [];
    demoConversation.forEach((_, i) => {
      const delay = 800 + i * 1600;
      const timer = setTimeout(() => {
        setVisibleMessages((prev) => prev + 1);
        setIsSpeaking(i % 2 === 0);
      }, delay);
      timers.push(timer);
    });

    return () => timers.forEach(clearTimeout);
  }, [phase]);

  const startCall = () => {
    if (selectedSkill === null) return;
    setPhase("calling");
  };

  const endCall = () => {
    setPhase("select");
    setVisibleMessages(0);
    setPartialText("");
    setIsSpeaking(false);
    setAmplitude(0);
  };

  return (
    <section
      id="voice-chat"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div
        className="glow-orb"
        style={{
          width: 400,
          height: 400,
          background:
            phase === "calling"
              ? "radial-gradient(circle, rgba(91,157,255,0.12))"
              : "radial-gradient(circle, rgba(136,226,206,0.12))",
          left: "50%",
          top: "50%",
          transform: "translate(-50%, -50%)",
          transition: "background 1s ease",
        }}
      />

      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center relative z-10">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">语音通话</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            全双工语音对话，让交流回归自然。
          </p>
          <p className="text-text-tertiary leading-relaxed mb-6">
            ASR 语音识别 → LLM 智能对话 → TTS 语音合成，三大 AI 能力无缝编排。
            支持实时打断、波形可视化，就像和真人通话一样自然。
          </p>

          <div className="flex flex-wrap gap-3">
            {["ASR 语音识别", "LLM 智能对话", "TTS 语音合成"].map(
              (tag, i) => (
                <span
                  key={tag}
                  className="stagger-item text-xs px-3 py-1.5 rounded-full glass"
                >
                  <span
                    className="w-1.5 h-1.5 rounded-full inline-block mr-1.5"
                    style={{
                      background: ["#88E2CE", "#5B9DFF", "#FFAEC9"][i],
                    }}
                  />
                  {tag}
                </span>
              )
            )}
          </div>
        </div>

        {/* Right: Voice Chat mockup */}
        <div className="glass-card p-5 w-full max-w-sm mx-auto min-h-[420px]">
          <AnimatePresence mode="wait">
            {phase === "select" ? (
              /* Phase 1: Skill Selection */
              <motion.div
                key="select"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                {/* Header */}
                <h3 className="text-sm font-semibold text-text-primary mb-1">
                  选择通话角色
                </h3>
                <p className="text-xs text-text-tertiary mb-4">
                  选择一个角色开始语音对话
                </p>

                {/* Skill grid */}
                <div className="grid grid-cols-3 gap-2 mb-4">
                  {demoSkills.map((skill, i) => (
                    <button
                      key={skill.name}
                      onClick={() =>
                        setSelectedSkill(i === selectedSkill ? null : i)
                      }
                      className="flex flex-col items-center gap-1.5 rounded-xl py-3 transition-all duration-300"
                      style={{
                        background:
                          i === selectedSkill
                            ? `${skill.color}22`
                            : "rgba(255,255,255,0.4)",
                        border:
                          i === selectedSkill
                            ? `1.5px solid ${skill.color}`
                            : "0.5px solid rgba(255,255,255,0.3)",
                      }}
                    >
                      <span className="text-2xl">{skill.emoji}</span>
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

                {/* Selected skill info */}
                <AnimatePresence>
                  {selectedSkill !== null && (
                    <motion.div
                      className="rounded-xl p-3 mb-4"
                      style={{
                        background: `${demoSkills[selectedSkill].color}15`,
                        border: `0.5px solid ${demoSkills[selectedSkill].color}30`,
                      }}
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                    >
                      <div className="flex items-center gap-2 mb-1">
                        <span>{demoSkills[selectedSkill].emoji}</span>
                        <span className="text-sm font-semibold text-text-primary">
                          {demoSkills[selectedSkill].name}
                        </span>
                      </div>
                      <p className="text-[10px] text-text-tertiary">
                        音色：温柔知性 · 模型：GPT-4o
                      </p>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Large mic button */}
                <div className="flex flex-col items-center gap-3">
                  <motion.button
                    onClick={startCall}
                    disabled={selectedSkill === null}
                    className="w-28 h-28 rounded-full flex items-center justify-center"
                    style={{
                      background:
                        selectedSkill !== null
                          ? "linear-gradient(135deg, #88E2CE, #5B9DFF)"
                          : "rgba(0,0,0,0.1)",
                    }}
                    whileHover={
                      selectedSkill !== null ? { scale: 1.05 } : undefined
                    }
                    whileTap={
                      selectedSkill !== null ? { scale: 0.95 } : undefined
                    }
                  >
                    <svg
                      className="w-12 h-12 text-white"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"
                      />
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M19 10v2a7 7 0 01-14 0v-2"
                      />
                      <line
                        x1="12"
                        y1="19"
                        x2="12"
                        y2="23"
                        strokeWidth={1.5}
                      />
                      <line
                        x1="8"
                        y1="23"
                        x2="16"
                        y2="23"
                        strokeWidth={1.5}
                      />
                    </svg>
                  </motion.button>
                  <span
                    className="text-xs"
                    style={{
                      color:
                        selectedSkill !== null
                          ? "var(--color-text-secondary)"
                          : "var(--color-text-tertiary)",
                    }}
                  >
                    {selectedSkill !== null ? "点击开始通话" : "请先选择一个角色"}
                  </span>
                </div>
              </motion.div>
            ) : (
              /* Phase 2: Call Active */
              <motion.div
                key="calling"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                {/* Call header */}
                <div className="flex items-center gap-3 mb-4 pb-3 border-b border-white/20">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center text-lg"
                    style={{
                      background: `${
                        selectedSkill !== null
                          ? demoSkills[selectedSkill].color
                          : "#88E2CE"
                      }33`,
                    }}
                  >
                    {selectedSkill !== null
                      ? demoSkills[selectedSkill].emoji
                      : "🤖"}
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-semibold text-text-primary">
                      {selectedSkill !== null
                        ? demoSkills[selectedSkill].name
                        : "AI 助理"}
                    </p>
                    <p className="text-[10px] text-[#88E2CE] flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#88E2CE] animate-pulse" />
                      通话中 · {isSpeaking ? "对方正在说话" : "倾听中"}
                    </p>
                  </div>
                  <button
                    onClick={endCall}
                    className="w-8 h-8 rounded-full bg-red-500/20 flex items-center justify-center hover:bg-red-500/30 transition-colors"
                  >
                    <svg
                      className="w-4 h-4 text-red-500"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M16 8l-8 8M8 8l8 8"
                      />
                    </svg>
                  </button>
                </div>

                {/* Model info */}
                <div className="rounded-xl p-2.5 mb-4 flex items-center gap-2 text-[10px] text-text-tertiary" style={{ background: "rgba(255,255,255,0.3)" }}>
                  <span>🎤 voice：温柔知性</span>
                  <span>·</span>
                  <span>🧠 模型：GPT-4o</span>
                </div>

                {/* Conversation */}
                <div className="space-y-3 min-h-[200px] mb-4">
                  {demoConversation
                    .slice(0, visibleMessages)
                    .map((msg, i) => (
                      <motion.div
                        key={i}
                        className={`flex ${
                          msg.role === "user"
                            ? "justify-end"
                            : "justify-start"
                        }`}
                        initial={{ opacity: 0, y: 12 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.3 }}
                      >
                        <div className="flex items-start gap-2 max-w-[85%]">
                          {msg.role === "ai" && (
                            <span className="text-xs mt-1">
                              {selectedSkill !== null
                                ? demoSkills[selectedSkill].emoji
                                : "🤖"}
                            </span>
                          )}
                          <div
                            className="rounded-xl px-3 py-2 text-xs leading-relaxed"
                            style={{
                              background:
                                msg.role === "user"
                                  ? "linear-gradient(135deg, #88E2CE, #5B9DFF)"
                                  : "rgba(255,255,255,0.25)",
                              color:
                                msg.role === "user"
                                  ? "white"
                                  : "var(--color-text-secondary)",
                            }}
                          >
                            {msg.role === "user" ? (
                              <span className="text-[10px] text-white/60 mr-1">
                                你
                              </span>
                            ) : null}
                            {msg.text}
                          </div>
                        </div>
                      </motion.div>
                    ))}

                  {/* ASR partial text */}
                  {partialText && (
                    <motion.div
                      className="flex justify-end"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                    >
                      <div className="rounded-xl px-3 py-2 text-xs italic text-text-tertiary"
                        style={{ background: "rgba(91,157,255,0.1)" }}>
                        {partialText}
                      </div>
                    </motion.div>
                  )}

                  {visibleMessages === 0 && !partialText && (
                    <div className="flex items-center justify-center h-[180px] text-text-tertiary text-xs">
                      等待对话开始…
                    </div>
                  )}
                </div>

                {/* Waveform */}
                <div className="flex flex-col items-center gap-2 pt-3 border-t border-white/20">
                  <Waveform isPlaying={isSpeaking} barCount={7} />
                  <span className="text-[10px] text-text-tertiary">
                    {isSpeaking ? "对方正在说话" : "静音"}
                  </span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </section>
  );
}
