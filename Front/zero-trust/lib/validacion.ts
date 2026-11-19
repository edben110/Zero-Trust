import type {
  Candidato,
  Catalogo,
  Comprobante,
  ResultadoCandidato,
  ResultadosGlobales,
  ResultadosRegionales,
} from "./contrato";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function esUuid(valor: string): boolean {
  return UUID.test(valor.trim());
}

// Misma regla que voting_api: NFKC, sin espacios ni puntos, de 6 a 10 dígitos. Conserva los ceros iniciales.
export function normalizarCedula(valor: string): string | null {
  const limpia = valor.normalize("NFKC").replace(/[\s.]/gu, "");
  return /^[0-9]{6,10}$/.test(limpia) ? limpia : null;
}

// Solo dígitos y máximo 10, también al pegar, para que el campo nunca quede con algo que el servidor rechace.
export function soloDigitos(valor: string): string {
  return valor.normalize("NFKC").replace(/[^0-9]/g, "").slice(0, 10);
}

export function leerVoterId(valor: unknown): string | null {
  if (!esObjeto(valor) || typeof valor.voterId !== "string" || !esUuid(valor.voterId)) {
    return null;
  }
  return valor.voterId.toLowerCase();
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esTexto(valor: unknown): valor is string {
  return typeof valor === "string" && valor.length > 0;
}

function esCandidato(valor: unknown): valor is Candidato {
  return esObjeto(valor) && esTexto(valor.candidateId) && esTexto(valor.name) && esTexto(valor.list);
}

export function leerCatalogo(valor: unknown): Catalogo | null {
  if (!esObjeto(valor) || !Array.isArray(valor.candidates) || !Array.isArray(valor.regions)) {
    return null;
  }
  if (!valor.candidates.every(esCandidato) || !valor.regions.every(esTexto)) {
    return null;
  }
  return { candidates: valor.candidates, regions: valor.regions };
}

export function leerComprobante(valor: unknown): Comprobante | null {
  if (!esObjeto(valor) || !esTexto(valor.voteId) || !esTexto(valor.receivedAt)) {
    return null;
  }
  return { voteId: valor.voteId, receivedAt: valor.receivedAt };
}

export function leerCamposInvalidos(valor: unknown): string[] {
  if (!esObjeto(valor) || !Array.isArray(valor.fields)) {
    return [];
  }
  return valor.fields.filter(esTexto);
}

function esResultado(valor: unknown): valor is ResultadoCandidato {
  return (
    esObjeto(valor) &&
    esTexto(valor.candidateId) &&
    typeof valor.candidateName === "string" &&
    typeof valor.voteCount === "number" &&
    Number.isFinite(valor.voteCount) &&
    valor.voteCount >= 0
  );
}

// Un worker puede reenviar el JSON como texto ya serializado; se acepta el objeto o su cadena.
function desenvolver(cuerpo: string): unknown {
  try {
    const primero: unknown = JSON.parse(cuerpo);
    return typeof primero === "string" ? JSON.parse(primero) : primero;
  } catch {
    return null;
  }
}

export function leerResultadosGlobales(cuerpo: string): ResultadosGlobales | null {
  const valor = desenvolver(cuerpo);
  if (!esObjeto(valor) || !esTexto(valor.timestamp) || !Array.isArray(valor.results)) {
    return null;
  }
  if (!valor.results.every(esResultado)) {
    return null;
  }
  return { timestamp: valor.timestamp, results: valor.results };
}

export function leerResultadosRegionales(cuerpo: string): ResultadosRegionales | null {
  const globales = leerResultadosGlobales(cuerpo);
  const valor = desenvolver(cuerpo);
  if (globales === null || !esObjeto(valor) || !esTexto(valor.region)) {
    return null;
  }
  return { ...globales, region: valor.region };
}
