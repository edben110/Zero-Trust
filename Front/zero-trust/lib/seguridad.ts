export type Umbrales = { suspicious: number; blocked: number; windowSeconds: number };
export type Resumen = {
  incidents: number;
  live: number;
  drills: number;
  duplicates: number;
  wrongRegion: number;
  ipsWithIncidents: number;
  currentRisk: Record<string, number>;
};
export type EstadoIp = {
  ip: string;
  riskLevel: string;
  voteCount: number;
  windowSeconds: number;
  firstSeen: string | null;
  lastSeen: string | null;
};
export type Incidente = {
  id: number;
  ip: string;
  kind: string;
  riskLevel: string;
  voteCount: number;
  windowSeconds: number;
  origin: string;
  detectedAt: string | null;
};
export type DatosSeguridad = { thresholds: Umbrales; summary: Resumen; ips: EstadoIp[]; incidents: Incidente[] };
export type Aviso = { texto: string; error: boolean };

export const NIVELES = ["NORMAL", "SOSPECHOSA", "BLOQUEADA"] as const;

const NIVEL: Record<string, { etiqueta: string; clase: string }> = {
  NORMAL: { etiqueta: "Normal", clase: "border-verificado text-verificado" },
  SOSPECHOSA: { etiqueta: "Sospechosa", clase: "border-sello text-sello" },
  BLOQUEADA: { etiqueta: "Bloqueada", clase: "border-papel bg-sello text-papel" },
};

const TIPO: Record<string, string> = {
  IP_PATTERN: "Patrón por IP",
  DUPLICATE_VOTE: "Voto duplicado",
  WRONG_REGION: "Región incorrecta",
};

export function tipo(valor: string) {
  return TIPO[valor] ?? valor ?? "—";
}

export function nivel(valor: string) {
  return NIVEL[valor] ?? { etiqueta: valor, clase: "border-tinta text-tinta" };
}

export function fecha(valor: string | null) {
  if (!valor) return "—";
  const instante = new Date(valor);
  return Number.isNaN(instante.getTime()) ? "—" : instante.toLocaleString("es-CO");
}

// Validación defensiva: lo que no cumpla la forma se descarta, no se inventa, y el panel no se rompe.
export function leerDatos(cuerpo: unknown): DatosSeguridad | null {
  if (typeof cuerpo !== "object" || cuerpo === null) return null;
  const datos = cuerpo as Record<string, unknown>;
  const umbrales = datos.thresholds as Record<string, unknown> | undefined;
  const resumen = datos.summary as Record<string, unknown> | undefined;
  if (!umbrales || typeof umbrales.suspicious !== "number" || !resumen) return null;
  const riesgo = (resumen.currentRisk ?? {}) as Record<string, unknown>;
  return {
    thresholds: {
      suspicious: Number(umbrales.suspicious),
      blocked: Number(umbrales.blocked),
      windowSeconds: Number(umbrales.windowSeconds),
    },
    summary: {
      incidents: Number(resumen.incidents ?? 0),
      live: Number(resumen.live ?? 0),
      drills: Number(resumen.drills ?? 0),
      duplicates: Number(resumen.duplicates ?? 0),
      wrongRegion: Number(resumen.wrongRegion ?? 0),
      ipsWithIncidents: Number(resumen.ipsWithIncidents ?? 0),
      currentRisk: {
        NORMAL: Number(riesgo.NORMAL ?? 0),
        SOSPECHOSA: Number(riesgo.SOSPECHOSA ?? 0),
        BLOQUEADA: Number(riesgo.BLOQUEADA ?? 0),
      },
    },
    ips: Array.isArray(datos.ips) ? (datos.ips as EstadoIp[]) : [],
    incidents: Array.isArray(datos.incidents) ? (datos.incidents as Incidente[]) : [],
  };
}
