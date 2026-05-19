"use client";

import { useEffect, useRef } from "react";

interface GlowOrb {
  color: string;
  size: number;
  animClass: string;
  initialX: string;
  initialY: string;
}

const orbs: GlowOrb[] = [
  {
    color: "rgba(136, 226, 206, 0.3)",
    size: 288,
    animClass: "animate-[float_8s_ease-in-out_infinite]",
    initialX: "10%",
    initialY: "15%",
  },
  {
    color: "rgba(255, 174, 201, 0.2)",
    size: 320,
    animClass: "animate-[float-delayed_8s_ease-in-out_infinite]",
    initialX: "70%",
    initialY: "30%",
  },
  {
    color: "rgba(91, 157, 255, 0.15)",
    size: 384,
    animClass: "animate-[float_8s_ease-in-out_infinite]",
    initialX: "40%",
    initialY: "60%",
  },
];

interface GlowBackgroundProps {
  accentColor?: string;
}

export default function GlowBackground({ accentColor }: GlowBackgroundProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  return (
    <div
      ref={containerRef}
      className="fixed inset-0 -z-10 overflow-hidden pointer-events-none"
      style={{ willChange: "transform" }}
    >
      {orbs.map((orb, i) => (
        <div
          key={i}
          className={`glow-orb ${orb.animClass}`}
          style={{
            width: orb.size,
            height: orb.size,
            background: accentColor
              ? `radial-gradient(circle, ${accentColor}${i === 0 ? "4D" : i === 1 ? "33" : "26"})`
              : `radial-gradient(circle, ${orb.color})`,
            left: orb.initialX,
            top: orb.initialY,
            opacity: 0.7,
            transition: "background 1.5s ease, opacity 1.5s ease",
          }}
        />
      ))}
    </div>
  );
}
