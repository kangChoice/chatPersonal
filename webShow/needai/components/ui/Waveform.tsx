"use client";

import { useEffect, useRef, useState } from "react";

interface WaveformProps {
  isPlaying: boolean;
  barCount?: number;
}

export default function Waveform({ isPlaying, barCount = 7 }: WaveformProps) {
  const [heights, setHeights] = useState<number[]>(
    Array.from({ length: barCount }, () => 4)
  );

  useEffect(() => {
    if (!isPlaying) {
      setHeights(Array.from({ length: barCount }, () => 4));
      return;
    }

    const interval = setInterval(() => {
      setHeights(
        Array.from(
          { length: barCount },
          () => Math.random() * 20 + 4
        )
      );
    }, 150);

    return () => clearInterval(interval);
  }, [isPlaying, barCount]);

  return (
    <div className="flex items-end gap-[3px] h-6">
      {heights.map((h, i) => (
        <div
          key={i}
          className="w-[3px] rounded-full transition-all duration-150"
          style={{
            height: `${h}px`,
            background: isPlaying
              ? "linear-gradient(180deg, #88E2CE, #5B9DFF)"
              : "rgba(255,255,255,0.3)",
          }}
        />
      ))}
    </div>
  );
}
