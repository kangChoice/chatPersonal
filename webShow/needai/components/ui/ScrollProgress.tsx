"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

const sections = [
  { id: "hero", label: "首页" },
  { id: "overview", label: "概述" },
  { id: "skill-carousel", label: "角色" },
  { id: "chat", label: "聊天" },
  { id: "multichat", label: "群聊" },
  { id: "skills", label: "管理" },
  { id: "voice", label: "音色" },
  { id: "voice-chat", label: "通话" },
  { id: "polish", label: "优化" },
  { id: "settings", label: "设置" },
  { id: "ilink", label: "微信接入" },
  { id: "footer", label: "下载" },
];

export default function ScrollProgress() {
  const [active, setActive] = useState(0);
  const [show, setShow] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      const scrollY = window.scrollY;
      const windowHeight = window.innerHeight;

      if (scrollY > windowHeight * 0.5) {
        setShow(true);
      } else {
        setShow(false);
      }

      const sectionElements = sections.map((s) =>
        document.getElementById(s.id)
      );

      for (let i = sectionElements.length - 1; i >= 0; i--) {
        const el = sectionElements[i];
        if (el && el.getBoundingClientRect().top < windowHeight * 0.6) {
          setActive(i);
          break;
        }
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const scrollTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          className="fixed right-6 top-1/2 -translate-y-1/2 z-50 flex flex-col gap-3"
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 20 }}
          transition={{ duration: 0.3 }}
        >
          {sections.map((s, i) => (
            <button
              key={s.id}
              onClick={() => scrollTo(s.id)}
              className="group relative flex items-center justify-center"
              aria-label={s.label}
            >
              <div
                className={`w-2 h-2 rounded-full transition-all duration-300 ${
                  i === active
                    ? "bg-[#5B9DFF] w-3 h-3"
                    : "bg-gray-300 hover:bg-gray-400"
                }`}
              />
              <span className="absolute right-5 px-2 py-0.5 text-xs whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 glass rounded-md">
                {s.label}
              </span>
            </button>
          ))}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
