import { NextRequest } from "next/server";
import { llamarApi, sesionValida } from "@/lib/admin-server";

export async function GET(req: NextRequest) {
  if (!sesionValida(req)) return new Response(null, { status: 401 });
  return llamarApi("GET", "/internal/admin/security/incidents");
}
