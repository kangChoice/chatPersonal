"use client";

import { cn } from "@/lib/utils";

interface GradientTextProps {
  children: React.ReactNode;
  as?: "h1" | "h2" | "h3" | "span" | "p";
  className?: string;
}

export default function GradientText({
  children,
  as: Tag = "span",
  className,
}: GradientTextProps) {
  return (
    <Tag className={cn("gradient-text", className)}>
      {children}
    </Tag>
  );
}
