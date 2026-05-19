"use client";

import Hero from "@/components/sections/Hero";
import Overview from "@/components/sections/Overview";
import SkillCarouselSection from "@/components/sections/SkillCarousel";
import ChatDemo from "@/components/sections/ChatDemo";
import MultiChatDemo from "@/components/sections/MultiChatDemo";
import SkillsDemo from "@/components/sections/SkillsDemo";
import VoiceDemo from "@/components/sections/VoiceDemo";
import VoiceChatDemo from "@/components/sections/VoiceChatDemo";
import PolishDemo from "@/components/sections/PolishDemo";
import SettingsDemo from "@/components/sections/SettingsDemo";
import Footer from "@/components/sections/Footer";
import ScrollProgress from "@/components/ui/ScrollProgress";

export default function Home() {
  return (
    <main className="relative min-h-screen">
      <Hero />
      <Overview />
      <SkillCarouselSection />
      <ChatDemo />
      <MultiChatDemo />
      <SkillsDemo />
      <VoiceDemo />
      <VoiceChatDemo />
      <PolishDemo />
      <SettingsDemo />
      <Footer />
      <ScrollProgress />
    </main>
  );
}
