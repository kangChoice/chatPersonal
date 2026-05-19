import { NextRequest, NextResponse } from "next/server";
import fs from "fs";
import path from "path";

const DATA_DIR = path.join(process.cwd(), "data");
const DATA_FILE = path.join(DATA_DIR, "visits.json");

// 内存计数器，避免并发写文件冲突
const counters: Record<string, number> = { pageview: 0, download: 0 };
let loaded = false;
let dirty = false;

function ensureLoaded() {
  if (loaded) return;
  try {
    const raw = fs.readFileSync(DATA_FILE, "utf-8");
    const data = JSON.parse(raw);
    counters.pageview = data.pageview ?? 0;
    counters.download = data.download ?? 0;
  } catch {
    counters.pageview = 0;
    counters.download = 0;
  }
  loaded = true;
}

function flushToDisk() {
  if (!dirty) return;
  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
    fs.writeFileSync(DATA_FILE, JSON.stringify(counters, null, 2), "utf-8");
    dirty = false;
  } catch {
    // 写失败下次再试
  }
}

// 每 30 秒自动落盘一次
const FLUSH_INTERVAL = 30_000;
let flushTimer: ReturnType<typeof setInterval> | null = null;

function startFlushTimer() {
  if (flushTimer) return;
  flushTimer = setInterval(flushToDisk, FLUSH_INTERVAL);
  // 不要让定时器阻止进程退出
  if (flushTimer && typeof flushTimer === "object" && "unref" in flushTimer) {
    flushTimer.unref();
  }
}

// 进程退出前落盘
function cleanup() {
  flushToDisk();
  if (flushTimer) {
    clearInterval(flushTimer);
    flushTimer = null;
  }
}
process.on("beforeExit", cleanup);
process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

export async function GET(request: NextRequest) {
  const type = request.nextUrl.searchParams.get("type") || "pageview";
  ensureLoaded();
  if (!(type in counters)) {
    return NextResponse.json({ error: "unknown type" }, { status: 400 });
  }
  return NextResponse.json({ count: counters[type], type });
}

export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => ({}));
  const type = body.type || "pageview";
  ensureLoaded();
  if (!(type in counters)) {
    return NextResponse.json({ error: "unknown type" }, { status: 400 });
  }
  counters[type]++;
  dirty = true;
  startFlushTimer();
  return NextResponse.json({ count: counters[type], type });
}
