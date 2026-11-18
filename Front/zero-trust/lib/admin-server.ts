import "server-only";
import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import type { NextRequest } from "next/server";

const COOKIE = "zt_admin";
const VIDA = 60 * 60;
let fallos = 0;
let ventana = 0;

function configuracion() {
  const clave = process.env.ADMIN_SESSION_SECRET;
  const password = process.env.ADMIN_PASSWORD;
  const apiToken = process.env.ADMIN_API_TOKEN;
  const apiUrl = process.env.ADMIN_API_URL;
  if (!clave || clave.length < 32 || !password || !apiToken || apiToken.length < 32 || !apiUrl) {
    throw new Error("Configure ADMIN_SESSION_SECRET, ADMIN_PASSWORD, ADMIN_API_TOKEN y ADMIN_API_URL");
  }
  return { clave, password, apiToken, apiUrl: apiUrl.replace(/\/+$/, "") };
}

export function accesoSeguro(req: NextRequest) {
  // ADMIN_HTTPS_READY solo debe activarse tras poner TLS delante del front y cerrar
  // el acceso directo a :3000: X-Forwarded-Proto no es fiable si el puerto es público.
  const local = req.nextUrl.hostname === "localhost" || req.nextUrl.hostname === "127.0.0.1";
  return local || (process.env.ADMIN_HTTPS_READY === "1" &&
    req.headers.get("x-forwarded-proto") === "https");
}

export function mismoOrigen(req: NextRequest) {
  const origen = req.headers.get("origin");
  if (!origen) return false;
  try {
    const url = new URL(origen);
    return url.host === req.headers.get("host") &&
      (url.protocol === "https:" || accesoSeguro(req));
  } catch { return false; }
}

function iguales(a: string, b: string) {
  const x = createHmac("sha256", "comparacion-admin").update(a).digest();
  const y = createHmac("sha256", "comparacion-admin").update(b).digest();
  return timingSafeEqual(x, y);
}

export function validarLogin(usuario: string, password: string) {
  const ahora = Date.now();
  if (ahora - ventana > 15 * 60_000) { ventana = ahora; fallos = 0; }
  if (fallos >= 5) return false;
  const valido = iguales(usuario, "admin") && iguales(password, configuracion().password);
  if (!valido) fallos++;
  else fallos = 0;
  return valido;
}

export function crearSesion() {
  const { clave } = configuracion();
  const dato = `${Math.floor(Date.now() / 1000) + VIDA}.${randomBytes(16).toString("hex")}`;
  const firma = createHmac("sha256", clave).update(dato).digest("hex");
  return `${dato}.${firma}`;
}

export function sesionValida(req: NextRequest) {
  const valor = req.cookies.get(COOKIE)?.value ?? "";
  const partes = valor.split(".");
  if (partes.length !== 3 || !/^\d+$/.test(partes[0]) ||
      !/^[0-9a-f]{32}$/.test(partes[1]) || !/^[0-9a-f]{64}$/.test(partes[2])) return false;
  const expiracion = Number(partes[0]);
  const ahora = Date.now() / 1000;
  if (expiracion < ahora || expiracion > ahora + VIDA + 60) return false;
  const firma = createHmac("sha256", configuracion().clave).update(`${partes[0]}.${partes[1]}`).digest("hex");
  return timingSafeEqual(Buffer.from(partes[2], "hex"), Buffer.from(firma, "hex"));
}

export const cookieAdmin = { name: COOKIE, httpOnly: true, secure: true,
  sameSite: "strict" as const, path: "/admin", maxAge: VIDA };

export async function llamarApi(metodo: string, ruta: string, body?: unknown) {
  const { apiUrl, apiToken } = configuracion();
  const respuesta = await fetch(`${apiUrl}${ruta}`, {
    method: metodo, cache: "no-store",
    headers: { "X-Admin-Token": apiToken, "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return Response.json(await respuesta.json().catch(() => ({ error: "Servicio no disponible." })),
    { status: respuesta.status, headers: { "Cache-Control": "no-store" } });
}
