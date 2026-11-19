import { NextRequest } from "next/server";
import { llamarApi, mismoOrigen, sesionValida } from "@/lib/admin-server";

export async function POST(req: NextRequest) {
  if (!sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  return llamarApi("POST", "/internal/admin/security/purge", {});
}
