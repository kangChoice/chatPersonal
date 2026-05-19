import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "NeedAI — 你的 AI 助理，不止于聊天",
  description:
    "NeedAIChat 是一款支持多角色、多模态、语音聊天的 AI 对话应用。流式对话、角色管理、音色克隆、提示词优化，尽在 NeedAI。",
  openGraph: {
    title: "NeedAI — 你的 AI 助理，不止于聊天",
    description:
      "多角色 AI 聊天、语音通话、音色克隆、提示词优化 —— 一个 App 搞定。",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" className={`${inter.variable}`}>
      <body>{children}</body>
    </html>
  );
}
