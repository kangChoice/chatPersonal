"use client";

import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  radius?: "card" | "sm" | "pill";
  hover?: boolean;
  style?: React.CSSProperties;
}

const radiusMap = {
  card: "24px",
  sm: "16px",
  pill: "999px",
};

export default function GlassCard({
  children,
  className,
  radius = "card",
  hover = false,
  style,
}: GlassCardProps) {
  return (
    <motion.div
      className={cn("glass-card", className)}
      style={{ borderRadius: radiusMap[radius], ...style }}
      whileHover={
        hover
          ? { y: -4, boxShadow: "0 12px 40px rgba(0,0,0,0.08)", transition: { duration: 0.3 } }
          : undefined
      }
    >
      {children}
    </motion.div>
  );
}
