"use client";

import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface GlassButtonProps {
  children: React.ReactNode;
  variant?: "default" | "nav" | "primary";
  className?: string;
  onClick?: () => void;
  href?: string;
}

export default function GlassButton({
  children,
  variant = "default",
  className,
  onClick,
  href,
}: GlassButtonProps) {
  const base =
    variant === "nav"
      ? "glass-nav-btn inline-flex items-center justify-center w-9 h-9 cursor-pointer"
      : variant === "primary"
      ? "inline-flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium cursor-pointer text-white rounded-full border-0"
      : "glass-button inline-flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium cursor-pointer";

  const bgStyle =
    variant === "primary"
      ? { background: "linear-gradient(135deg, #5B9DFF, #88E2CE)" }
      : {};

  const content = (
    <motion.div
      className={cn(base, className)}
      style={bgStyle}
      onClick={onClick}
      whileHover={{ scale: 1.03, boxShadow: "0 4px 20px rgba(91,157,255,0.3)" }}
      whileTap={{ scale: 0.97 }}
    >
      {children}
    </motion.div>
  );

  if (href) {
    return <a href={href} target="_blank" rel="noopener noreferrer">{content}</a>;
  }

  return content;
}
