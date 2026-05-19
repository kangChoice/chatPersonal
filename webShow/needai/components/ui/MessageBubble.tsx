"use client";

import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface MessageBubbleProps {
  content: string;
  role: "user" | "ai";
  className?: string;
  delay?: number;
}

export default function MessageBubble({
  content,
  role,
  className,
  delay = 0,
}: MessageBubbleProps) {
  const isUser = role === "user";

  return (
    <motion.div
      className={cn(
        "flex",
        isUser ? "justify-end" : "justify-start",
        className
      )}
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.4, delay, ease: [0.25, 0.1, 0.25, 1] }}
    >
      {!isUser && (
        <div className="w-7 h-7 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-xs mr-2 mt-1 shrink-0 border border-white/10">
          🤖
        </div>
      )}
      <div
        className={cn(
          "max-w-[80%] px-4 py-2.5 text-sm leading-6",
          isUser
            ? "rounded-[20px] rounded-tr-[4px] text-white"
            : "rounded-[20px] rounded-tl-[4px] text-white/90"
        )}
        style={
          isUser
            ? {
                background: "linear-gradient(135deg, #88E2CE, #5B9DFF)",
              }
            : {
                background: "rgba(0,0,0,0.45)",
                border: "0.5px solid rgba(255,255,255,0.1)",
              }
        }
      >
        {content}
      </div>
      {isUser && (
        <div className="w-7 h-7 rounded-full bg-white/70 backdrop-blur-sm flex items-center justify-center text-xs ml-2 mt-1 shrink-0 border border-white/30">
          😊
        </div>
      )}
    </motion.div>
  );
}
