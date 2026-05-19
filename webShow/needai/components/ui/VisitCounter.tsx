"use client";

import { useEffect, useState, useRef } from "react";

export default function VisitCounter() {
  const [visits, setVisits] = useState<number | null>(null);
  const [downloads, setDownloads] = useState<number | null>(null);
  const [error, setError] = useState(false);
  const tracked = useRef(false);

  useEffect(() => {
    // 页面访问计数 +1（仅首次）
    if (!tracked.current) {
      tracked.current = true;
      fetch("/api/visits", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ type: "pageview" }),
      });
    }

    // 读取当前数据
    const load = async () => {
      try {
        const [v, d] = await Promise.all([
          fetch("/api/visits?type=pageview").then((r) => r.json()),
          fetch("/api/visits?type=download").then((r) => r.json()),
        ]);
        setVisits(v.count);
        setDownloads(d.count);
      } catch {
        setError(true);
      }
    };
    load();
  }, []);

  if (error) return null;

  return (
    <div className="flex items-center gap-4 text-[10px] text-text-tertiary/40">
      <span className="flex items-center gap-1">
        <svg
          className="w-3 h-3"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
        访问 {visits ?? "—"}
      </span>
      <span className="flex items-center gap-1">
        <svg
          className="w-3 h-3"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3" />
        </svg>
        下载 {downloads ?? "—"}
      </span>
    </div>
  );
}
