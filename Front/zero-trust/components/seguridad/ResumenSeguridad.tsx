import { NIVELES, nivel, type Resumen, type Umbrales } from "@/lib/seguridad";
import { InsigniaNivel } from "./InsigniaNivel";

export function ResumenSeguridad({ thresholds, summary }: { thresholds: Umbrales; summary: Resumen }) {
  return (
    <>
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
        {NIVELES.map((clave) => (
          <InsigniaNivel key={clave} valor={clave}>
            {nivel(clave).etiqueta}: <span className="cifras">{summary.currentRisk[clave] ?? 0}</span>
          </InsigniaNivel>
        ))}
      </p>

      <p className="mt-2 text-sm text-tinta-suave">
        Por tipo: patrón por IP <strong className="cifras">{summary.incidents - summary.duplicates - summary.wrongRegion}</strong>
        {" "}· voto duplicado <strong className="cifras">{summary.duplicates}</strong>
        {" "}· región incorrecta <strong className="cifras">{summary.wrongRegion}</strong>.
      </p>
    </>
  );
}
