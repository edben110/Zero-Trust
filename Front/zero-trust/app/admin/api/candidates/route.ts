import { NextRequest } from "next/server";
import { accesoSeguro, llamarApi, mismoOrigen, sesionValida } from "@/lib/admin-server";

export async function GET(req: NextRequest) {
  if (!accesoSeguro(req) || !sesionValida(req)) return new Response(null, { status: 401 });
  // El listado utiliza el mismo catálogo público que usa el tarjetón.
  return llamarApi("GET", "/catalog");
}

export async function POST(req: NextRequest) {
  if (!accesoSeguro(req) || !sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  const body = await req.json().catch(() => null);
  if (typeof body?.name !== "string" || typeof body?.list !== "string") {
    return Response.json({ error: "Nombre y lista son obligatorios." }, { status: 400 });
  }
  return llamarApi("POST", "/internal/admin/candidates", { name: body.name, list: body.list });
}
