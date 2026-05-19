"use client";

import { useState, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import { useSectionTransition } from "@/components/animations/sectionTransition";

const demoSkills = [
  {
    name: "学习助手",
    emoji: "📚",
    color: "#88E2CE",
    description: "专为学生打造的 AI 导师，帮你理解复杂概念、梳理知识体系。",
    greeting: "来，我们一起搞定这道题",
    tags: ["学习", "辅导", "知识"],
  },
  {
    name: "编程导师",
    emoji: "💻",
    color: "#5B9DFF",
    description: "全栈 AI 编程助手，支持代码审查、Debug、架构设计。",
    greeting: "又遇到 bug 了？让我看看",
    tags: ["编程", "调试", "技术"],
  },
  {
    name: "心理树洞",
    emoji: "🌿",
    color: "#FFAEC9",
    description: "温暖的倾听者，提供情绪支持和心理疏导建议。",
    greeting: "我在听，慢慢说",
    tags: ["心理", "倾诉", "温暖"],
  },
];

export default function SkillCarouselSection() {
  const [activeIndex, setActiveIndex] = useState(0);
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  const active = demoSkills[activeIndex];

  return (
    <section
      id="skill-carousel"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div
        className="glow-orb"
        style={{
          width: 500,
          height: 500,
          background: `radial-gradient(circle, ${active.color}15)`,
          left: "50%",
          top: "50%",
          transform: "translate(-50%, -50%)",
          transition: "background 0.8s ease",
        }}
      />

      <div className="section-content max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 items-center relative z-10">
        {/* Left: Text */}
        <div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">首页大卡片</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3 leading-relaxed">
            多角色 AI 助理，一键切换。
          </p>
          <p className="text-text-tertiary mb-8 leading-relaxed">
            每个角色都有独立的人设、语气和知识领域。
            从学习辅导到编程调试，从心理树洞到英语陪练，
            总有一个角色懂你所需。
          </p>

          {/* Skill selector pills */}
          <div className="flex gap-3 flex-wrap">
            {demoSkills.map((skill, i) => (
              <button
                key={i}
                onClick={() => setActiveIndex(i)}
                className="stagger-item px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-300"
                style={{
                  background:
                    i === activeIndex
                      ? `${skill.color}33`
                      : "rgba(255,255,255,0.7)",
                  border:
                    i === activeIndex
                      ? `1.5px solid ${skill.color}`
                      : "0.5px solid rgba(255,255,255,0.5)",
                  color: i === activeIndex ? skill.color : "#4A5568",
                }}
              >
                {skill.emoji} {skill.name}
              </button>
            ))}
          </div>
        </div>

        {/* Right: Card stack */}
        <div className="relative h-[400px] flex items-center justify-center">
          {demoSkills.map((skill, i) => {
            const isActive = i === activeIndex;
            const offset = (i - activeIndex) * 12;

            return (
              <motion.div
                key={skill.name}
                className="absolute glass-card p-6 w-[300px] cursor-pointer select-none"
                animate={{
                  y: isActive ? 0 : offset + 60,
                  scale: isActive ? 1 : 0.85,
                  rotateZ: isActive ? 0 : (i - activeIndex) * 3,
                  opacity: isActive ? 1 : 0.4,
                  zIndex: isActive ? 10 : 5 - Math.abs(i - activeIndex),
                }}
                transition={{ duration: 0.5, ease: [0.25, 0.1, 0.25, 1] }}
                onClick={() => setActiveIndex(i)}
                style={{
                  background: isActive
                    ? `linear-gradient(135deg, ${skill.color}22, rgba(255,255,255,0.7))`
                    : "rgba(255,255,255,0.7)",
                }}
              >
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-3xl">{skill.emoji}</span>
                  <div>
                    <h3 className="text-lg font-semibold text-text-primary">
                      {skill.name}
                    </h3>
                    <div className="flex gap-1.5 mt-1">
                      {skill.tags.map((tag) => (
                        <span
                          key={tag}
                          className="text-[10px] px-2 py-0.5 rounded-full"
                          style={{
                            background: `${skill.color}22`,
                            color: skill.color,
                          }}
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
                {isActive && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    transition={{ duration: 0.3 }}
                  >
                    <p className="text-sm text-text-secondary mb-3 leading-relaxed">
                      {skill.description}
                    </p>
                    <div
                      className="rounded-xl px-4 py-2.5 text-sm italic"
                      style={{
                        background: `${skill.color}15`,
                        border: `0.5px solid ${skill.color}30`,
                      }}
                    >
                      💬 "{skill.greeting}"
                    </div>
                  </motion.div>
                )}
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
