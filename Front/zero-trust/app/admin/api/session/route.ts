import { NextRequest, NextResponse } from "next/server";
import { accesoSeguro, cookieAdmin, crearSesion, mismoOrigen, sesionValida, validarLogin } from "@/lib/admin-server";

export async function GET(req: NextRequest) {
  return NextResponse.json({ autenticado: accesoSeguro(req) && sesionValida(req) },
    { headers: { "Cache-Control": "no-store" } });
}

export async function POST(req: NextRequest) {
  if (!accesoSeguro(req)) return NextResponse.json({ error: "La administración requiere HTTPS." }, { status: 403 });
  if (!mismoOrigen(req)) return NextResponse.json({ error: "Origen no permitido." }, { status: 403 });
  const datos = await req.json().catch(() => null);
  if (typeof datos?.usuario !== "string" || typeof datos?.password !== "string" ||
      !validarLogin(datos.usuario, datos.password)) {
    return NextResponse.json({ error: "Credenciales incorrectas o demasiados intentos." }, { status: 401 });
  }
  const res = NextResponse.json({ autenticado: true });
  res.cookies.set({ ...cookieAdmin, value: crearSesion(),
    secure: req.nextUrl.hostname !== "localhost" && req.nextUrl.hostname !== "127.0.0.1" });
  return res;
}

export async function DELETE(req: NextRequest) {
  if (!mismoOrigen(req)) return new Response(null, { status: 403 });
  const res = NextResponse.json({ autenticado: false });
  res.cookies.set({ ...cookieAdmin, value: "", maxAge: 0,
    secure: req.nextUrl.hostname !== "localhost" && req.nextUrl.hostname !== "127.0.0.1" });
  return res;
}
