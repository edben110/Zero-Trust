import "server-only";
import { createHmac, randomBytes, scryptSync, timingSafeEqual } from "node:crypto";
import type { NextRequest } from "next/server";

const COOKIE = "zt_admin";
const VIDA = 60 * 60;
let fallos = 0;
let ventana = 0;
// El panel se ejecuta como una sola instancia de Next.js en vm-app. Cada login
// reemplaza esta sesión, invalidando inmediatamente la cookie del dispositivo
// anterior; no confiar solo en la expiración firmada de la cookie.
let sesionActiva: string | null = null;

function configuracion() {
  const clave = process.env.ADMIN_SESSION_SECRET;
  const passwordHash = process.env.ADMIN_PASSWORD_HASH;
  const apiToken = process.env.ADMIN_API_TOKEN;
  const apiUrl = process.env.ADMIN_API_URL;
  if (!clave || clave.length < 32 || !/^scrypt:[a-f0-9]{32}:[a-f0-9]{128}$/.test(passwordHash ?? "") ||
      !apiToken || apiToken.length < 32 || !apiUrl) {
    throw new Error("Configure ADMIN_SESSION_SECRET, ADMIN_PASSWORD_HASH, ADMIN_API_TOKEN y ADMIN_API_URL");
  }
  return { clave, passwordHash: passwordHash!, apiToken, apiUrl: apiUrl.replace(/\/+$/, "") };
}

export function mismoOrigen(req: NextRequest) {
  const origen = req.headers.get("origin");
  if (!origen) return false;
  try {
    const url = new URL(origen);
    return url.host === req.headers.get("host") && url.protocol === req.nextUrl.protocol;
  } catch { return false; }
}

function iguales(a: string, b: string) {
  const x = createHmac("sha256", "comparacion-admin").update(a).digest();
  const y = createHmac("sha256", "comparacion-admin").update(b).digest();
  return timingSafeEqual(x, y);
}

export function validarLogin(usuario: string, password: string): "ok" | "incorrecto" | "limitado" {
  const ahora = Date.now();
  if (ahora - ventana > 15 * 60_000) { ventana = ahora; fallos = 0; }
  if (fallos >= 5) return "limitado";
  const [, salHex, claveHex] = configuracion().passwordHash.split(":");
  const esperado = Buffer.from(claveHex, "hex");
  const calculado = scryptSync(password, Buffer.from(salHex, "hex"), esperado.length,
    { N: 16384, r: 8, p: 1, maxmem: 64 * 1024 * 1024 });
  const valido = iguales(usuario, "admin") && timingSafeEqual(calculado, esperado);
  if (!valido) fallos++;
  else fallos = 0;
  return valido ? "ok" : "incorrecto";
}

export function crearSesion() {
  const { clave } = configuracion();
  const idSesion = randomBytes(16).toString("hex");
  sesionActiva = idSesion;
  const dato = `${Math.floor(Date.now() / 1000) + VIDA}.${idSesion}`;
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
  if (expiracion < ahora || expiracion > ahora + VIDA + 60 || partes[1] !== sesionActiva) return false;
  const firma = createHmac("sha256", configuracion().clave).update(`${partes[0]}.${partes[1]}`).digest("hex");
  return timingSafeEqual(Buffer.from(partes[2], "hex"), Buffer.from(firma, "hex"));
}

export function cerrarSesion(req: NextRequest) {
  const valor = req.cookies.get(COOKIE)?.value ?? "";
  const idSesion = valor.split(".")[1];
  // Un logout de una cookie vieja no debe cerrar la sesión más reciente.
  if (idSesion && idSesion === sesionActiva) sesionActiva = null;
}

export const cookieAdmin = { name: COOKIE, httpOnly: true, secure: false,
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
