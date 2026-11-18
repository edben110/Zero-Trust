import { NextRequest } from "next/server";
import { llamarApi, mismoOrigen, sesionValida } from "@/lib/admin-server";

// Alta en el padrón. La cédula solo pasa por aquí en el cuerpo de la petición: no se registra ni se guarda.
export async function POST(req: NextRequest) {
  if (!sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  const body = await req.json().catch(() => null);
  if (typeof body?.cedula !== "string" || typeof body?.region !== "string") {
    return Response.json({ error: "Cédula y región son obligatorias." }, { status: 400 });
  }
  return llamarApi("POST", "/internal/admin/voters", {
    cedula: body.cedula, region: body.region,
  });
}
