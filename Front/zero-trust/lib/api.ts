import { config } from "./config";
import type { Catalogo, ResultadoConsulta, ResultadoVoto, SolicitudVoto } from "./contrato";
import { catalogoDemo, consultarDemo, votarDemo } from "./demo";
import { leerCamposInvalidos, leerCatalogo, leerComprobante, leerVoterId } from "./validacion";

export async function obtenerCatalogo(): Promise<Catalogo> {
  if (config.demo) {
    return catalogoDemo;
  }
  const respuesta = await fetch(`${config.votingApiUrl}/catalog`, { cache: "no-store" });
  if (!respuesta.ok) {
    throw new Error("catalogo-no-disponible");
  }
  const catalogo = leerCatalogo(await respuesta.json());
  if (catalogo === null) {
    throw new Error("catalogo-invalido");
  }
  return catalogo;
}

async function cuerpoJson(respuesta: Response): Promise<unknown> {
  try {
    return await respuesta.json();
  } catch {
    return null;
  }
}

export async function enviarVoto(solicitud: SolicitudVoto): Promise<ResultadoVoto> {
  if (config.demo) {
    return votarDemo(solicitud);
  }
  let respuesta: Response;
  try {
    respuesta = await fetch(`${config.votingApiUrl}/vote`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(solicitud),
    });
  } catch {
    return { tipo: "no-disponible" };
  }
  const cuerpo = await cuerpoJson(respuesta);
  if (respuesta.status === 200) {
    const comprobante = leerComprobante(cuerpo);
    return comprobante === null ? { tipo: "error" } : { tipo: "aceptado", comprobante };
  }
  if (respuesta.status === 400) {
    return { tipo: "invalido", campos: leerCamposInvalidos(cuerpo) };
  }
  if (respuesta.status === 403) {
    return { tipo: "no-habilitado" };
  }
  if (respuesta.status === 409) {
    return { tipo: "ya-voto" };
  }
  if (respuesta.status === 503) {
    return { tipo: "no-disponible" };
  }
  return { tipo: "error" };
}

// La cédula se envía por POST en el cuerpo y no se guarda en el navegador. cache "no-store": la respuesta
// tampoco debe quedar en la caché del navegador.
export async function consultarCodigo(cedula: string): Promise<ResultadoConsulta> {
  if (config.demo) {
    return consultarDemo(cedula);
  }
  let respuesta: Response;
  try {
    respuesta = await fetch(`${config.votingApiUrl}/voter-id`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ cedula }),
      cache: "no-store",
    });
  } catch {
    return { tipo: "no-disponible" };
  }
  if (respuesta.status === 200) {
    const voterId = leerVoterId(await cuerpoJson(respuesta));
    return voterId === null ? { tipo: "error" } : { tipo: "encontrado", voterId };
  }
  if (respuesta.status === 400) {
    return { tipo: "invalido" };
  }
  if (respuesta.status === 404) {
    return { tipo: "no-registrado" };
  }
  if (respuesta.status === 503) {
    return { tipo: "no-disponible" };
  }
  return { tipo: "error" };
}
