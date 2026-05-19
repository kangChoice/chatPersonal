"use client";

import Image from "next/image";

interface AppLogoProps {
  size?: number;
  className?: string;
}

export default function AppLogo({ size = 64, className }: AppLogoProps) {
  return (
    <Image
      src="/app_icon.webp"
      alt="NeedAI"
      width={size}
      height={size}
      className={className}
      style={{ borderRadius: Math.max(size * 0.2, 8), objectFit: "contain" }}
      priority
    />
  );
}
