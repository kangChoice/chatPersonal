"use client";

import { useEffect, useRef } from "react";
import { motion } from "framer-motion";
import { gsap } from "gsap";
import GradientText from "@/components/ui/GradientText";
import GlassButton from "@/components/ui/GlassButton";
import GlowBackground from "@/components/ui/GlowBackground";
import AppLogo from "@/components/ui/AppLogo";

export default function Hero() {
  const containerRef = useRef<HTMLDivElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    const ctx = gsap.context(() => {
      const letters = titleRef.current?.querySelectorAll(".letter");
      if (letters) {
        gsap.from(letters, {
          opacity: 0,
          y: 40,
          rotateX: -90,
          stagger: 0.04,
          duration: 0.6,
          ease: "back.out(1.7)",
          delay: 0.3,
        });
      }

      gsap.from(".hero-sub", {
        opacity: 0,
        y: 20,
        duration: 0.8,
        delay: 1.0,
        ease: "power2.out",
      });

      gsap.from(".hero-desc", {
        opacity: 0,
        y: 20,
        duration: 0.8,
        delay: 1.2,
        ease: "power2.out",
      });

      gsap.from(".hero-cta", {
        opacity: 0,
        y: 20,
        duration: 0.8,
        delay: 1.4,
        ease: "power2.out",
      });

      gsap.from(".hero-scroll", {
        opacity: 0,
        duration: 0.8,
        delay: 1.8,
        ease: "power2.out",
      });
    }, containerRef);

    return () => ctx.revert();
  }, []);

  const title = "NeedAI";
  const titleLetters = title.split("");

  return (
    <section
      id="hero"
      ref={containerRef}
      className="relative min-h-screen flex flex-col items-center justify-center overflow-hidden"
    >
      <GlowBackground />

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center text-center px-6">
        {/* App Icon */}
        <motion.div
          className="mb-8"
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 0.8, ease: [1.34, 1.1, 0.4, 1.3] }}
        >
          <AppLogo size={72} />
        </motion.div>

        {/* Title with letter animation */}
        <h1
          ref={titleRef}
          className="text-7xl sm:text-8xl md:text-9xl font-bold tracking-tight mb-4"
        >
          {titleLetters.map((letter, i) => (
            <span
              key={i}
              className="letter inline-block gradient-text"
              style={{ display: "inline-block" }}
            >
              {letter === " " ? " " : letter}
            </span>
          ))}
        </h1>

        {/* Subtitle */}
        <p className="hero-sub text-2xl sm:text-3xl font-semibold gradient-text mb-3">
          需要爱
        </p>

        {/* Description */}
        <p className="hero-desc text-lg sm:text-xl text-text-secondary max-w-lg mb-10">
          你的 AI 助理，不止于聊天。
          <br />
          想你所想
          —— 做你想做
        </p>

        {/* CTA Buttons */}
        <div className="hero-cta flex gap-4">
          <GlassButton
            variant="primary"
            onClick={() =>
              document
                .getElementById("overview")
                ?.scrollIntoView({ behavior: "smooth" })
            }
          >
            <svg
              className="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
            探索功能
          </GlassButton>
          <GlassButton
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
            <svg
              className="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
            下载 App
          </GlassButton>
        </div>
      </div>

      {/* Scroll indicator */}
      <div className="hero-scroll absolute bottom-10 flex flex-col items-center gap-2 text-text-tertiary">
        <span className="text-xs tracking-widest">向下滚动</span>
        <div className="w-5 h-8 rounded-full border border-text-tertiary/30 flex justify-center pt-1.5">
          <div className="w-1 h-2 rounded-full bg-text-tertiary/50 animate-[scroll-indicator_1.5s_ease-in-out_infinite]" />
        </div>
      </div>
    </section>
  );
}
