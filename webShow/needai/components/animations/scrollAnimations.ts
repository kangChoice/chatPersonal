"use client";

import { useEffect, useRef } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

gsap.registerPlugin(ScrollTrigger);

export function useSectionAnimation(
  ref: React.RefObject<HTMLDivElement | null>,
  options?: {
    from?: gsap.TweenVars;
    to?: gsap.TweenVars;
    trigger?: gsap.DOMTarget;
  }
) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const ctx = gsap.context(() => {
      gsap.from(el, {
        scrollTrigger: {
          trigger: options?.trigger || el,
          start: "top 85%",
          end: "top 40%",
          toggleActions: "play none none reverse",
        },
        y: 60,
        opacity: 0,
        duration: 0.8,
        ease: "power3.out",
        ...options?.from,
      });
    });

    return () => ctx.revert();
  }, [ref, options]);
}

export function useTitleAnimation(ref: React.RefObject<HTMLDivElement | null>) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const ctx = gsap.context(() => {
      gsap.from(el.querySelectorAll(".anim-title"), {
        scrollTrigger: {
          trigger: el,
          start: "top 85%",
          end: "top 40%",
          toggleActions: "play none none reverse",
        },
        y: 40,
        opacity: 0,
        stagger: 0.15,
        duration: 0.7,
        ease: "power2.out",
      });

      gsap.from(el.querySelectorAll(".anim-content"), {
        scrollTrigger: {
          trigger: el,
          start: "top 80%",
          end: "top 40%",
          toggleActions: "play none none reverse",
        },
        y: 30,
        opacity: 0,
        stagger: 0.1,
        duration: 0.6,
        ease: "power2.out",
        delay: 0.2,
      });
    });

    return () => ctx.revert();
  }, [ref]);
}
