import { NextRequest, NextResponse } from "next/server";
import fs from "fs";
import path from "path";

const DATA_DIR = path.join(process.cwd(), "data");
const DATA_FILE = path.join(DATA_DIR, "visits.json");

function readCounters(): Record<string, number> {
  try {
    const raw = fs.readFileSync(DATA_FILE, "utf-8");
    return JSON.parse(raw);
  } catch {
    return { pageview: 0, download: 0 };
  }
}

function writeCounters(counters: Record<string, number>) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(counters, null, 2), "utf-8");
}

// GET: 只读，不增加
export async function GET(request: NextRequest) {
  const type = request.nextUrl.searchParams.get("type") || "pageview";
  const counters = readCounters();
  if (!(type in counters)) {
    return NextResponse.json({ error: "unknown type" }, { status: 400 });
  }
  return NextResponse.json({ count: counters[type], type });
}

// POST: +1 并返回新值
export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => ({}));
  const type = body.type || "pageview";
  const counters = readCounters();
  if (!(type in counters)) {
    return NextResponse.json({ error: "unknown type" }, { status: 400 });
  }
  counters[type]++;
  writeCounters(counters);
  return NextResponse.json({ count: counters[type], type });
}
