"use client";

import { useState, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import GradientText from "@/components/ui/GradientText";
import GlassCard from "@/components/ui/GlassCard";
import { useSectionTransition } from "@/components/animations/sectionTransition";

interface Skill {
  name: string;
  emoji: string;
  desc: string;
  tags: string[];
  color: string;
}

const initialSkills: Skill[] = [
  { name: "学习导师", emoji: "📚", desc: "全科知识辅导，耐心讲解", tags: ["学习", "辅导"], color: "#88E2CE" },
  { name: "编程教练", emoji: "💻", desc: "代码审查、Bug 修复、架构设计", tags: ["编程", "技术"], color: "#5B9DFF" },
  { name: "心理树洞", emoji: "🌿", desc: "温暖倾听，情绪支持", tags: ["心理", "倾诉"], color: "#FFAEC9" },
  { name: "英语陪练", emoji: "🗣️", desc: "口语对话、语法纠错、写作润色", tags: ["语言", "练习"], color: "#88E2CE" },
  { name: "创意助手", emoji: "✨", desc: "头脑风暴、文案创作、方案策划", tags: ["创意", "写作"], color: "#5B9DFF" },
  { name: "面试官", emoji: "🎯", desc: "模拟面试、简历优化、职业规划", tags: ["求职", "面试"], color: "#FFAEC9" },
];

export default function SkillsDemo() {
  const [skills, setSkills] = useState(initialSkills);
  const [showNewForm, setShowNewForm] = useState(false);
  const [newSkillName, setNewSkillName] = useState("");
  const [newSkillEmoji, setNewSkillEmoji] = useState("🌟");
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  const addSkill = () => {
    if (!newSkillName.trim()) return;
    const colors = ["#88E2CE", "#5B9DFF", "#FFAEC9"];
    setSkills([
      ...skills,
      {
        name: newSkillName,
        emoji: newSkillEmoji,
        desc: "自定义角色，由你定义",
        tags: ["自定义"],
        color: colors[skills.length % 3],
      },
    ]);
    setNewSkillName("");
    setShowNewForm(false);
  };

  const deleteSkill = (index: number) => {
    setSkills(skills.filter((_, i) => i !== index));
  };

  return (
    <section
      id="skills"
      ref={sectionRef}
      className="relative min-h-screen flex items-center justify-center px-6 py-32"
    >
      <div className="section-content max-w-6xl w-full mx-auto">
        {/* Header */}
        <div className="text-center mb-16">
          <h2 className="text-4xl sm:text-5xl font-bold mb-6">
            <GradientText as="span">角色管理</GradientText>
          </h2>
          <p className="text-lg text-text-secondary mb-3">你的 AI 角色工坊</p>
          <p className="text-text-tertiary max-w-lg mx-auto">
            创建、编辑、管理你的 AI 角色。每个角色都可以自定义人设、语气和知识领域，让 AI 真正懂你。
          </p>
        </div>

        {/* Add button */}
        <div className="flex justify-center mb-10">
          <button
            onClick={() => setShowNewForm(!showNewForm)}
            className="stagger-item glass-button px-6 py-3 text-sm font-medium flex items-center gap-2"
          >
            <span className="text-lg">+</span>
            创建新角色
          </button>
        </div>

        {/* New skill form */}
        <AnimatePresence>
          {showNewForm && (
            <motion.div
              className="max-w-md mx-auto mb-10 glass-card p-6"
              initial={{ opacity: 0, y: -20, height: 0 }}
              animate={{ opacity: 1, y: 0, height: "auto" }}
              exit={{ opacity: 0, y: -20, height: 0 }}
            >
              <div className="flex gap-3 mb-4">
                <input
                  value={newSkillEmoji}
                  onChange={(e) => setNewSkillEmoji(e.target.value)}
                  className="w-12 text-center text-xl rounded-xl border-0 bg-white/50"
                />
                <input
                  value={newSkillName}
                  onChange={(e) => setNewSkillName(e.target.value)}
                  placeholder="输入角色名称…"
                  className="flex-1 px-4 py-2 rounded-xl border-0 bg-white/50 text-sm outline-none focus:ring-2 focus:ring-[#5B9DFF]/30"
                  onKeyDown={(e) => e.key === "Enter" && addSkill()}
                />
              </div>
              <button
                onClick={addSkill}
                className="w-full py-2 rounded-full text-sm font-medium text-white"
                style={{
                  background: "linear-gradient(135deg, #88E2CE, #5B9DFF)",
                }}
              >
                确认创建
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Skill grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <AnimatePresence>
            {skills.map((skill, i) => (
              <motion.div
                key={skill.name + skill.color}
                layout
                initial={{ opacity: 0, scale: 0.8, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.8, y: -20 }}
                transition={{ duration: 0.4, delay: i * 0.05 }}
              >
                <GlassCard className="p-5" hover>
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <span className="text-2xl">{skill.emoji}</span>
                      <div>
                        <h3 className="font-semibold text-text-primary text-sm">{skill.name}</h3>
                        <div className="flex gap-1 mt-1">
                          {skill.tags.map((tag) => (
                            <span
                              key={tag}
                              className="text-[10px] px-2 py-0.5 rounded-full"
                              style={{ background: `${skill.color}22`, color: skill.color }}
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => deleteSkill(i)}
                      className="text-text-tertiary/50 hover:text-red-400 transition-colors text-xs"
                    >
                      ✕
                    </button>
                  </div>
                  <p className="text-xs text-text-tertiary">{skill.desc}</p>
                </GlassCard>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>
    </section>
  );
}
