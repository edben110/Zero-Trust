import { NextRequest } from "next/server";
import { accesoSeguro, llamarApi, mismoOrigen, sesionValida } from "@/lib/admin-server";

type Contexto = { params: Promise<{ id: string }> };
export async function PUT(req: NextRequest, { params }: Contexto) {
  if (!accesoSeguro(req) || !sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  const body = await req.json().catch(() => null);
  if (typeof body?.name !== "string" || typeof body?.list !== "string") {
    return Response.json({ error: "Nombre y lista son obligatorios." }, { status: 400 });
  }
  const { id } = await params;
  if (!/^[0-9a-f-]{36}$/i.test(id)) return new Response(null, { status: 400 });
  return llamarApi("PUT", `/internal/admin/candidates/${id}`, { name: body.name, list: body.list });
}

export async function DELETE(req: NextRequest, { params }: Contexto) {
  if (!accesoSeguro(req) || !sesionValida(req) || !mismoOrigen(req)) return new Response(null, { status: 403 });
  const { id } = await params;
  if (!/^[0-9a-f-]{36}$/i.test(id)) return new Response(null, { status: 400 });
  return llamarApi("DELETE", `/internal/admin/candidates/${id}`);
}
