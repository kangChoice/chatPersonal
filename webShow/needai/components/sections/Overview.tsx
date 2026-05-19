"use client";

import { useRef } from "react";
import { motion } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import { useTitleAnimation } from "@/components/animations/scrollAnimations";

const highlights = [
  {
    number: "多角色",
    label: "AI 助理",
    desc: "学习、编程、心理、创意… 每个场景都有专属角色",
    color: "#88E2CE",
  },
  {
    number: "多模态",
    label: "对话方式",
    desc: "文本聊天、语音通话、群聊讨论，随心切换",
    color: "#5B9DFF",
  },
  {
    number: "200+",
    label: "系统音色",
    desc: "从温柔知性到活力治愈，总有一款适合你",
    color: "#FFAEC9",
  },
];

export default function Overview() {
  const sectionRef = useRef<HTMLDivElement>(null);
  useTitleAnimation(sectionRef as React.RefObject<HTMLDivElement | null>);

  return (
    <section
      id="overview"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="max-w-5xl w-full mx-auto">
        <div className="text-center mb-20">
          <h2 className="anim-title text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">什么是 NeedAI？</GradientText>
          </h2>
          <p className="anim-content text-lg text-text-secondary max-w-2xl mx-auto leading-relaxed">
            NeedAI 是一款面向未来的 AI 对话应用。它不仅支持流畅的流式聊天，
            更将角色扮演、多角色群聊、语音通话和音色管理融合在一个优雅的体验中。
            <br />
            让 AI 真正成为你工作、学习和生活中的得力伙伴。
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {highlights.map((item, i) => (
            <motion.div
              key={i}
              className="stagger-item glass-card p-8 text-center"
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.6, delay: i * 0.15 }}
              whileHover={{ y: -6, transition: { duration: 0.3 } }}
            >
              <div
                className="text-5xl font-bold mb-3"
                style={{ color: item.color }}
              >
                {item.number}
              </div>
              <h3 className="text-lg font-semibold text-text-primary mb-2">
                {item.label}
              </h3>
              <p className="text-sm text-text-tertiary leading-relaxed">
                {item.desc}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
