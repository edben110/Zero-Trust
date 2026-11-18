import { NextRequest, NextResponse } from "next/server";
import { cerrarSesion, cookieAdmin, crearSesion, mismoOrigen, sesionValida, validarLogin } from "@/lib/admin-server";

export async function GET(req: NextRequest) {
  return NextResponse.json({ autenticado: sesionValida(req) },
    { headers: { "Cache-Control": "no-store" } });
}

export async function POST(req: NextRequest) {
  if (!mismoOrigen(req)) return NextResponse.json({ error: "Origen no permitido." }, { status: 403 });
  const datos = await req.json().catch(() => null);
  if (typeof datos?.usuario !== "string" || typeof datos?.password !== "string" ||
      datos.usuario.length > 64 || datos.password.length > 256) {
    return NextResponse.json({ error: "Credenciales incorrectas." }, { status: 401 });
  }
  const resultado = validarLogin(datos.usuario, datos.password);
  if (resultado === "limitado") {
    return NextResponse.json({ error: "Demasiados intentos. Espere 15 minutos." },
      { status: 429, headers: { "Retry-After": "900", "Cache-Control": "no-store" } });
  }
  if (resultado !== "ok") {
    return NextResponse.json({ error: "Credenciales incorrectas." },
      { status: 401, headers: { "Cache-Control": "no-store" } });
  }
  const res = NextResponse.json({ autenticado: true }, { headers: { "Cache-Control": "no-store" } });
  res.cookies.set({ ...cookieAdmin, value: crearSesion() });
  return res;
}

export async function DELETE(req: NextRequest) {
  if (!mismoOrigen(req)) return new Response(null, { status: 403 });
  cerrarSesion(req);
  const res = NextResponse.json({ autenticado: false });
  res.cookies.set({ ...cookieAdmin, value: "", maxAge: 0, expires: new Date(0) });
  return res;
}
