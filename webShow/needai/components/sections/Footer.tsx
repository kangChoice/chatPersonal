"use client";

import { useRef } from "react";
import GradientText from "@/components/ui/GradientText";
import GlassButton from "@/components/ui/GlassButton";
import GlowBackground from "@/components/ui/GlowBackground";
import AppLogo from "@/components/ui/AppLogo";
import VisitCounter from "@/components/ui/VisitCounter";
import { useSectionTransition } from "@/components/animations/sectionTransition";

export default function Footer() {
  const sectionRef = useRef<HTMLDivElement>(null);
  useSectionTransition(sectionRef as React.RefObject<HTMLDivElement | null>);

  return (
    <section
      id="footer"
      ref={sectionRef}
      className="relative min-h-[80vh] flex flex-col items-center justify-center px-6 py-32 overflow-hidden"
    >
      <GlowBackground accentColor="#5B9DFF" />

      <div className="section-content relative z-10 flex flex-col items-center text-center max-w-2xl mx-auto">
        {/* CTA */}
        {/* App Icon */}
        <div className="mb-8">
          <AppLogo size={64} />
        </div>

        <h2 className="text-4xl sm:text-5xl font-bold mb-6">
          <GradientText as="span">开始使用 NeedAI</GradientText>
        </h2>

        <p className="text-lg text-text-secondary mb-10 max-w-md leading-relaxed">
          准备好体验下一代 AI 对话了吗？
          <br />
          多角色、多模态、语音通话 —— 完全本地存储。
        </p>

        <p className="text-lg text-text-secondary mb-10 max-w-md leading-relaxed">
                 给您极致的隐私体验。
        </p>

        <div className="flex gap-4 mb-16">
          <GlassButton
            variant="primary"
            onClick={() => {
              fetch("/api/visits", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ type: "download" }),
              });
              window.open(
                "https://github.com/kangChoice/chatPersonal/releases/download/version1.4.0/NeedAIChat.apk",
                "_blank"
              );
            }}
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
            下载 App
          </GlassButton>
          <GlassButton href="https://github.com/kangChoice/chatPersonal">
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
            </svg>
            GitHub
          </GlassButton>
        </div>

        {/* Links */}
        <div className="flex flex-wrap justify-center gap-6 mb-8 text-xs text-text-tertiary">
          <a href="https://space.bilibili.com/438288916" target="_blank" rel="noopener noreferrer" className="hover:text-text-secondary transition-colors">
            联系方式
          </a>
          <a
            href="https://space.bilibili.com/438288916"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-text-secondary transition-colors"
            title="哔哩哔哩"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.813 4.653h.854c1.51.054 2.769.578 3.773 1.574 1.004.995 1.524 2.249 1.562 3.76v7.36c-.038 1.51-.558 2.765-1.562 3.76s-2.262 1.52-3.773 1.575H5.333c-1.51-.054-2.769-.579-3.773-1.575-1.004-.995-1.524-2.249-1.562-3.76v-7.36c.038-1.511.558-2.765 1.562-3.76 1.004-.996 2.262-1.52 3.773-1.574h.774l-1.174-1.12a1.234 1.234 0 0 1-.373-.906c0-.356.124-.658.373-.907.249-.248.551-.377.907-.386.355.009.657.138.906.386L9.333 4.653h5.334l1.413-1.374c.249-.248.551-.377.907-.386.355.009.657.138.906.386.249.249.373.551.373.907 0 .355-.124.657-.373.906l-1.08 1.12v.001zM5.333 7.42a1.65 1.65 0 0 0-1.186.48c-.321.32-.481.71-.481 1.173v7.334c0 .462.16.852.48 1.173.321.32.711.48 1.187.48h13.334c.475 0 .865-.16 1.186-.48.321-.32.481-.71.481-1.173V9.073c0-.462-.16-.852-.48-1.173-.321-.32-.711-.48-1.187-.48H5.333zm6.667 2a.82.82 0 0 1 .853.787v1.546c0 .462-.284.695-.853.7-.57-.005-.854-.238-.854-.7v-1.546c0-.462.284-.695.854-.787zm-4 0a.82.82 0 0 1 .853.787v1.546c0 .462-.284.695-.853.7-.57-.005-.854-.238-.854-.7v-1.546c0-.462.284-.695.854-.787z"/>
            </svg>
          </a>
          <a
            href="https://github.com/kangChoice/chatPersonal"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-text-secondary transition-colors"
          >
            开源仓库
          </a>
        </div>

        <p className="text-xs text-text-tertiary/60">
          © 2026 NeedAIChat. Made with ❤️.
        </p>
        <div className="mt-4">
          <VisitCounter />
        </div>
      </div>
    </section>
  );
}
