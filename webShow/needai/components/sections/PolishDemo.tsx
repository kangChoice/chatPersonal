"use client";

import { useState, useEffect, useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import { useSectionTransition } from "@/components/animations/sectionTransition";

const beforeText = "你是一个AI助手，帮我回答问题，要详细一些，不要敷衍。";
const afterText =
  "你是一位专业且富有洞察力的 AI 助手。请以友好、专业的语气回应用户的问题。回答应当：\n\n1. **准确且有深度**：基于事实，提供有实质内容的回答\n2. **结构清晰**：适当使用分段、列表等格式\n3. **示例丰富**：配合实例帮助理解抽象概念\n4. **引导思考**：在回答的最后，提出 1-2 个延伸问题激发进一步讨论";

export default function PolishDemo() {
  const [isPolishing, setIsPolishing] = useState(false);
  const [showResult, setShowResult] = useState(false);
  const [displayText, setDisplayText] = useState("");
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  const handlePolish = () => {
    setIsPolishing(true);
    setShowResult(false);
    setDisplayText("");

    setTimeout(() => {
      setIsPolishing(false);
      setShowResult(true);
    }, 1200);
  };

  useEffect(() => {
    if (!showResult) return;
    let i = 0;
    const interval = setInterval(() => {
      if (i < afterText.length) {
        setDisplayText(afterText.slice(0, i + 1));
        i++;
      } else {
        clearInterval(interval);
      }
    }, 20);
    return () => clearInterval(interval);
  }, [showResult]);

  return (
    <section
      id="polish"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto">
        {/* Header */}
        <div className="text-center mb-16">
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">提示词优化</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3">写出更好的提示词</p>
          <p className="text-text-tertiary max-w-lg mx-auto">
            用 AI 优化你的提示词。输入粗略想法，获得结构清晰、表达精准的专业提示词。
          </p>
        </div>

        {/* Before / After comparison */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 max-w-4xl mx-auto">
          {/* Before */}
          <div className="glass-card p-6">
            <h3 className="text-sm font-semibold text-text-tertiary mb-3 flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-red-100 flex items-center justify-center text-[10px]">
                ✗
              </span>
              优化前
            </h3>
            <div
              className="rounded-xl p-4 text-sm leading-relaxed text-text-secondary"
              style={{
                background: "rgba(255,255,255,0.5)",
                border: "0.5px solid rgba(255,255,255,0.5)",
              }}
            >
              {beforeText}
            </div>
          </div>

          {/* After */}
          <div className="glass-card p-6">
            <h3 className="text-sm font-semibold text-text-tertiary mb-3 flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-[#88E2CE]/30 flex items-center justify-center text-[10px]">
                ✓
              </span>
              优化后
            </h3>
            <div
              className="rounded-xl p-4 text-sm leading-relaxed min-h-[120px] whitespace-pre-line"
              style={{
                background: "rgba(136, 226, 206, 0.08)",
                border: "0.5px solid rgba(136, 226, 206, 0.2)",
                color: "#2D3748",
              }}
            >
              {displayText}
              {showResult && displayText.length < afterText.length && (
                <span className="inline-block w-[2px] h-4 bg-[#5B9DFF] ml-0.5 animate-pulse" />
              )}
              {!showResult && !isPolishing && (
                <span className="text-text-tertiary">等待优化…</span>
              )}
              {isPolishing && (
                <span className="text-text-tertiary flex items-center gap-2">
                  <span className="w-4 h-4 rounded-full border-2 border-[#5B9DFF] border-t-transparent animate-spin" />
                  AI 正在优化…
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Polish button */}
        <div className="flex justify-center mt-8">
          <motion.button
            onClick={handlePolish}
            disabled={isPolishing}
            className="stagger-item glass-button px-8 py-3 text-sm font-medium flex items-center gap-2"
            whileHover={{ scale: 1.03 }}
            whileTap={{ scale: 0.97 }}
          >
            {isPolishing ? (
              <>
                <span className="w-4 h-4 rounded-full border-2 border-[#5B9DFF] border-t-transparent animate-spin" />
                优化中…
              </>
            ) : (
              <>
                <span>✨</span>
                优化提示词
              </>
            )}
          </motion.button>
        </div>
      </div>
    </section>
  );
}
