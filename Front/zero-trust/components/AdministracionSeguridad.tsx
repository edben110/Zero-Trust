"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";

const campo = "w-full border border-tinta bg-papel px-3 py-2 outline-offset-2 focus:outline focus:outline-sello";
const boton = "border-2 border-tinta bg-tinta px-5 py-2 font-medium text-papel hover:bg-papel hover:text-tinta disabled:opacity-50";

type Umbrales = { suspicious: number; blocked: number; windowSeconds: number };
type Resumen = {
  incidents: number;
  live: number;
  drills: number;
  duplicates: number;
  wrongRegion: number;
  ipsWithIncidents: number;
  currentRisk: Record<string, number>;
};
type EstadoIp = {
  ip: string;
  riskLevel: string;
  voteCount: number;
  windowSeconds: number;
  firstSeen: string | null;
  lastSeen: string | null;
};
type Incidente = {
  id: number;
  ip: string;
  kind: string;
  riskLevel: string;
  voteCount: number;
  windowSeconds: number;
  origin: string;
  detectedAt: string | null;
};
type Datos = { thresholds: Umbrales; summary: Resumen; ips: EstadoIp[]; incidents: Incidente[] };

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

function tipo(valor: string) {
  return TIPO[valor] ?? valor ?? "—";
}

function nivel(valor: string) {
  return NIVEL[valor] ?? { etiqueta: valor, clase: "border-tinta text-tinta" };
}

function fecha(valor: string | null) {
  if (!valor) return "—";
  const instante = new Date(valor);
  return Number.isNaN(instante.getTime()) ? "—" : instante.toLocaleString("es-CO");
}

// Validación defensiva: el panel no debe romperse si el servicio responde
// parcialmente. Lo que no cumpla la forma se descarta, no se inventa.
function leerDatos(cuerpo: unknown): Datos | null {
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

export function AdministracionSeguridad() {
  const [datos, setDatos] = useState<Datos | null>(null);
  const [ip, setIp] = useState("");
  const [votos, setVotos] = useState("12");
  const [aviso, setAviso] = useState<{ texto: string; error: boolean } | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const descargar = useCallback(async (): Promise<Datos | null> => {
    try {
      const res = await fetch("/admin/api/security/incidents", { cache: "no-store" });
      if (res.status === 401 || res.status === 403) return null;
      return res.ok ? leerDatos(await res.json()) : null;
    } catch {
      // El refresco automático es best-effort; el aviso lo da una acción explícita.
      return null;
    }
  }, []);

  const cargar = useCallback(async () => {
    const leidos = await descargar();
    if (leidos) setDatos(leidos);
  }, [descargar]);

  useEffect(() => {
    // La lectura inicial y el sondeo son asíncronos: el estado solo cambia
    // cuando llega la respuesta, nunca durante el render del efecto.
    let cancelado = false;
    const refrescar = async () => {
      const leidos = await descargar();
      if (!cancelado && leidos) setDatos(leidos);
    };
    void refrescar();
    const intervalo = window.setInterval(() => { void refrescar(); }, 3000);
    return () => { cancelado = true; window.clearInterval(intervalo); };
  }, [descargar]);

  async function llamar(ruta: string, cuerpo: Record<string, unknown>, exito: string) {
    setOcupado(true);
    setAviso(null);
    try {
      const res = await fetch(ruta, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cuerpo),
      });
      const respuesta = await res.json().catch(() => null);
      if (!res.ok) {
        setAviso({ texto: respuesta?.error ?? "No se pudo completar la operación.", error: true });
        return;
      }
      setAviso({ texto: exito, error: false });
      // El consumidor de Kafka aplica el patrón State de forma asíncrona:
      // se da un margen corto antes de releer el estado proyectado.
      await new Promise((listo) => window.setTimeout(listo, 800));
      await cargar();
    } catch {
      setAviso({ texto: "No se pudo comunicar con el servidor.", error: true });
    } finally {
      setOcupado(false);
    }
  }

  async function simular(evento: FormEvent) {
    evento.preventDefault();
    const cantidad = Number(votos);
    if (!Number.isInteger(cantidad) || cantidad < 1 || cantidad > 50) {
      setAviso({ texto: "El número de votos debe estar entre 1 y 50.", error: true });
      return;
    }
    await llamar("/admin/api/security/drills", { ip: ip.trim(), votes: cantidad },
      `Prueba enviada: ${cantidad} voto(s) desde ${ip.trim()}. El estado se actualizará en unos segundos.`);
  }

  async function reiniciar() {
    if (!window.confirm(`¿Devolver ${ip.trim() || "la IP"} a NORMAL? Se descarta su estado en memoria; el histórico de incidentes se conserva.`)) return;
    await llamar("/admin/api/security/reset", { ip: ip.trim() },
      `Reinicio enviado para ${ip.trim()}. La IP vuelve a NORMAL y sus contadores a cero.`);
  }

  // El sondeo cada 3 s ya refresca la vista; este botón fuerza la lectura y
  // deja constancia visible de que sí se consultó el servidor.
  async function actualizar() {
    setOcupado(true);
    setAviso(null);
    const leidos = await descargar();
    if (leidos) {
      setDatos(leidos);
      setAviso({ texto: `Estado actualizado a las ${new Date().toLocaleTimeString("es-CO")}.`, error: false });
    } else {
      setAviso({ texto: "No se pudo actualizar. Verifique la sesión y vuelva a iniciar sesión.", error: true });
    }
    setOcupado(false);
  }

  async function purgar() {
    if (!window.confirm("Se eliminarán TODOS los incidentes y los estados por IP. Los contadores quedarán en cero y la acción no se puede deshacer. ¿Continuar?")) return;
    await llamar("/admin/api/security/purge", {},
      "Incidentes y estados por IP eliminados. Los contadores quedaron en cero.");
  }

  if (!datos) return (
    <section aria-label="Detección de ataques" className="mt-12 border-t-2 border-tinta pt-8">
      <h2 className="font-serif text-2xl">Detección de ataques por IP</h2>
      <p className="mt-2 text-sm text-tinta-suave">Consultando el estado del detector…</p>
    </section>
  );

  const { thresholds, summary, ips, incidents } = datos;

  return (
    <section aria-labelledby="titulo-seguridad" className="mt-12 border-t-2 border-tinta pt-8">
      <h2 id="titulo-seguridad" className="font-serif text-2xl">Detección de ataques por IP</h2>
      <p className="mt-2 max-w-3xl text-sm text-tinta-suave">
        Las pruebas publican en <code className="font-mono">security_drills</code> y el detector aplica el
        mismo patrón State que usa con el tráfico real: <strong>NORMAL</strong> → <strong>SOSPECHOSA</strong>
        {" "}al superar {thresholds.suspicious} votos en {thresholds.windowSeconds} s → <strong>BLOQUEADA</strong>
        {" "}al superar {thresholds.blocked}. No se escriben votos en <code className="font-mono">raw_votes</code>:
        el escrutinio y la auditoría electoral no se alteran.
      </p>

      <dl className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ["Incidentes", summary.incidents],
          ["Reales (LIVE)", summary.live],
          ["Pruebas (DRILL)", summary.drills],
          ["IPs con incidentes", summary.ipsWithIncidents],
        ].map(([etiqueta, valor]) => (
          <div key={String(etiqueta)} className="border border-tinta px-4 py-3">
            <dt className="text-xs font-semibold uppercase tracking-wider text-tinta-suave">{etiqueta}</dt>
            <dd className="cifras mt-1 font-serif text-2xl">{valor}</dd>
          </div>
        ))}
      </dl>

      <p className="mt-4 flex flex-wrap items-center gap-2 text-sm">
        <span className="font-medium">Estado vigente:</span>
        {(["NORMAL", "SOSPECHOSA", "BLOQUEADA"] as const).map((clave) => (
          <span key={clave} className={`border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${nivel(clave).clase}`}>
            {nivel(clave).etiqueta}: <span className="cifras">{summary.currentRisk[clave] ?? 0}</span>
          </span>
        ))}
      </p>

      <p className="mt-2 text-sm text-tinta-suave">
        Por tipo: patrón por IP <strong className="cifras">{summary.incidents - summary.duplicates - summary.wrongRegion}</strong>
        {" "}· voto duplicado <strong className="cifras">{summary.duplicates}</strong>
        {" "}· región incorrecta <strong className="cifras">{summary.wrongRegion}</strong>.
      </p>

      <form onSubmit={simular} className="mt-8 grid gap-5 border border-tinta p-6 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <label className="block text-sm font-medium">Dirección IP real o de prueba
          <input className={`${campo} mt-2 font-mono`} value={ip} onChange={(e) => setIp(e.target.value)}
            placeholder="203.0.113.10" autoComplete="off" spellCheck={false} required />
        </label>
        <label className="block text-sm font-medium">Votos simulados
          <input className={`${campo} mt-2 cifras`} type="number" min={1} max={50} value={votos}
            onChange={(e) => setVotos(e.target.value)} required />
        </label>
        <div className="flex flex-wrap items-center gap-4 sm:col-span-2">
          <button className={boton} disabled={ocupado}>{ocupado ? "Enviando…" : "Ejecutar prueba"}</button>
          <button type="button" onClick={() => void reiniciar()} disabled={ocupado}
            className="border-2 border-sello px-5 py-2 font-medium text-sello disabled:opacity-50">
            Restablecer IP
          </button>
          <button type="button" onClick={() => void actualizar()} disabled={ocupado} className="text-sm underline underline-offset-4">
            {ocupado ? "Actualizando…" : "Actualizar ahora"}
          </button>
          <button type="button" onClick={() => void purgar()} disabled={ocupado}
            className="ml-auto border-2 border-sello bg-sello px-5 py-2 font-medium text-papel hover:bg-papel hover:text-sello disabled:opacity-50">
            Borrar incidentes y estados
          </button>
        </div>
        <div aria-live="polite" className="sm:col-span-2">
          {aviso && <p role={aviso.error ? "alert" : "status"} className={aviso.error ? "text-sello" : "font-medium"}>{aviso.texto}</p>}
        </div>
      </form>

      <h3 className="mt-10 border-b border-tinta pb-2 font-serif text-xl">IPs observadas ({ips.length})</h3>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[40rem] border-collapse text-sm">
          <thead>
            <tr className="border-b border-tinta text-left">
              <th className="py-2 pr-3 font-medium">IP</th>
              <th className="py-2 pr-3 font-medium">Estado</th>
              <th className="py-2 pr-3 font-medium">Votos</th>
              <th className="py-2 pr-3 font-medium">Primera vez</th>
              <th className="py-2 font-medium">Última vez</th>
            </tr>
          </thead>
          <tbody>
            {ips.length === 0 && <tr><td colSpan={5} className="py-4 text-tinta-suave">Aún no hay tráfico evaluado.</td></tr>}
            {ips.map((fila) => (
              <tr key={fila.ip} className="border-b border-tinta/20">
                <td className="py-2 pr-3 font-mono">{fila.ip}</td>
                <td className="py-2 pr-3">
                  <span className={`border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${nivel(fila.riskLevel).clase}`}>
                    {nivel(fila.riskLevel).etiqueta}
                  </span>
                </td>
                <td className="cifras py-2 pr-3">{fila.voteCount}</td>
                <td className="py-2 pr-3">{fecha(fila.firstSeen)}</td>
                <td className="py-2">{fecha(fila.lastSeen)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3 className="mt-10 border-b border-tinta pb-2 font-serif text-xl">Incidentes recientes ({incidents.length})</h3>
      <p className="mt-2 text-sm text-tinta-suave">
        Registro en PostgreSQL. <strong>LIVE</strong> es tráfico real; <strong>DRILL</strong> es una
        prueba lanzada desde este panel. El botón «Borrar incidentes y estados» vacía este histórico.
      </p>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[52rem] border-collapse text-sm">
          <thead>
            <tr className="border-b border-tinta text-left">
              <th className="py-2 pr-3 font-medium">Detectado</th>
              <th className="py-2 pr-3 font-medium">IP</th>
              <th className="py-2 pr-3 font-medium">Tipo</th>
              <th className="py-2 pr-3 font-medium">Estado</th>
              <th className="py-2 pr-3 font-medium">Votos</th>
              <th className="py-2 font-medium">Origen</th>
            </tr>
          </thead>
          <tbody>
            {incidents.length === 0 && <tr><td colSpan={6} className="py-4 text-tinta-suave">Sin incidentes registrados.</td></tr>}
            {incidents.map((fila) => (
              <tr key={fila.id} className="border-b border-tinta/20">
                <td className="py-2 pr-3">{fecha(fila.detectedAt)}</td>
                <td className="py-2 pr-3 font-mono">{fila.ip}</td>
                <td className="py-2 pr-3">{tipo(fila.kind)}</td>
                <td className="py-2 pr-3">
                  {fila.kind === "IP_PATTERN" ? (
                    <span className={`border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${nivel(fila.riskLevel).clase}`}>
                      {nivel(fila.riskLevel).etiqueta}
                    </span>
                  ) : <span className="text-tinta-suave">—</span>}
                </td>
                <td className="cifras py-2 pr-3">{fila.voteCount}</td>
                <td className="py-2 font-mono text-xs">{fila.origin}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
