"use client";

import { useEffect } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

gsap.registerPlugin(ScrollTrigger);

export function useSectionTransition(
  ref: React.RefObject<HTMLDivElement | null>,
  options?: {
    color?: string;
  }
) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const ctx = gsap.context(() => {
      const content = el.querySelector(".section-content") as HTMLElement;

      if (content) {
        gsap.fromTo(
          content,
          {
            opacity: 0.6,
            scale: 0.95,
          },
          {
            scrollTrigger: {
              trigger: el,
              start: "top 70%",
              end: "top 30%",
              toggleActions: "play none none reverse",
            },
            opacity: 1,
            scale: 1,
            duration: 1.2,
            ease: "power2.out",
          }
        );

        gsap.fromTo(
          content.querySelectorAll(".stagger-item"),
          {
            opacity: 0,
            y: 30,
          },
          {
            scrollTrigger: {
              trigger: el,
              start: "top 65%",
              end: "top 35%",
              toggleActions: "play none none reverse",
            },
            opacity: 1,
            y: 0,
            stagger: 0.08,
            duration: 0.6,
            ease: "power2.out",
          }
        );
      }
    });

    return () => ctx.revert();
  }, [ref, options?.color]);
}
