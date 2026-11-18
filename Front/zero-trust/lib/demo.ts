// Modo demostración (NEXT_PUBLIC_DEMO=1): datos simulados, siempre acompañados del aviso "Datos simulados".
import type { Catalogo, ResultadoConsulta, ResultadoVoto, ResultadosGlobales, SolicitudVoto } from "./contrato";
import { normalizarCedula } from "./validacion";

export const CODIGO_DEMO = "5d1e2f30-4a5b-4c6d-8e7f-9a0b1c2d3e4f";

export const CEDULA_DEMO = "1020304051";

const PADRON_DEMO = new Set([
  CODIGO_DEMO,
  "6e2f3041-5b6c-4d7e-9f80-a1b2c3d4e5f6",
  "7f304152-6c7d-4e8f-8091-b2c3d4e5f607",
]);

// En la demostración el código de las cédulas de prueba es fijo; el servidor real lo calcula con HMAC.
const CODIGOS_DEMO = new Map([
  [CEDULA_DEMO, CODIGO_DEMO],
  ["1020304052", "6e2f3041-5b6c-4d7e-9f80-a1b2c3d4e5f6"],
  ["1020304053", "7f304152-6c7d-4e8f-8091-b2c3d4e5f607"],
]);

const YA_VOTARON = new Set<string>();

export const catalogoDemo: Catalogo = {
  candidates: [
    { candidateId: "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8", name: "Lucía Marín Ospina", list: "Movimiento Ceiba" },
    { candidateId: "1c2d3e4f-5061-4b72-8c83-a4b5c6d7e8f9", name: "Andrés Felipe Quintero", list: "Alianza Río Claro" },
    { candidateId: "2d3e4f50-6172-4c83-9d94-b5c6d7e8f90a", name: "Valentina Rojas Ibarra", list: "Coalición Guadua" },
    { candidateId: "3e4f5061-7283-4d94-8ea5-c6d7e8f90a1b", name: "Samuel Ortega Lozano", list: "Partido de los Páramos" },
  ],
  regions: ["norte", "sur", "oriente", "occidente", "centro"],
};

function esperar(ms: number): Promise<void> {
  return new Promise((resolver) => setTimeout(resolver, ms));
}

export async function votarDemo(solicitud: SolicitudVoto): Promise<ResultadoVoto> {
  await esperar(700);
  const voterId = solicitud.voterId.toLowerCase();
  if (!PADRON_DEMO.has(voterId)) {
    return { tipo: "no-habilitado" };
  }
  if (YA_VOTARON.has(voterId)) {
    return { tipo: "ya-voto" };
  }
  YA_VOTARON.add(voterId);
  return {
    tipo: "aceptado",
    comprobante: { voteId: crypto.randomUUID(), receivedAt: new Date().toISOString() },
  };
}

export async function consultarDemo(cedula: string): Promise<ResultadoConsulta> {
  await esperar(500);
  const normalizada = normalizarCedula(cedula);
  if (normalizada === null) {
    return { tipo: "invalido" };
  }
  return { tipo: "encontrado", voterId: CODIGOS_DEMO.get(normalizada) ?? crypto.randomUUID() };
}

// Simula los conteos que publicarían los dashboard workers, creciendo cada pocos segundos.
export function simularResultados(semilla: number, alActualizar: (resultados: ResultadosGlobales) => void): () => void {
  const conteos = catalogoDemo.candidates.map((_, indice) => ((semilla * 37 + indice * 53) % 90) + 10);
  const emitir = () =>
    alActualizar({
      timestamp: new Date().toISOString(),
      results: catalogoDemo.candidates.map((candidato, indice) => ({
        candidateId: candidato.candidateId,
        candidateName: candidato.candidateId,
        voteCount: conteos[indice],
      })),
    });
  const primero = setTimeout(emitir, 400);
  const intervalo = setInterval(() => {
    const indice = Math.floor(Math.random() * conteos.length);
    conteos[indice] += 1 + Math.floor(Math.random() * 4);
    emitir();
  }, 2500);
  return () => {
    clearTimeout(primero);
    clearInterval(intervalo);
  };
}
