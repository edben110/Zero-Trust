import { NextRequest } from "next/server";
import { llamarApi, mismoOrigen, sesionValida } from "@/lib/admin-server";

const IP = /^[0-9a-fA-F:.]{2,45}$/;

export async function POST(req: NextRequest) {
  if (!sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  const body = await req.json().catch(() => null);
  const ip = typeof body?.ip === "string" ? body.ip.trim() : "";
  if (!IP.test(ip)) {
    return Response.json({ error: "Indique una dirección IP válida." }, { status: 400 });
  }
  return llamarApi("POST", "/internal/admin/security/reset", { ip });
}
