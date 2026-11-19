import { fecha, tipo, type Incidente } from "@/lib/seguridad";
import { InsigniaNivel } from "./InsigniaNivel";

export function TablaIncidentes({ incidents }: { incidents: Incidente[] }) {
  return (
    <>
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
                  {fila.kind === "IP_PATTERN" ? <InsigniaNivel valor={fila.riskLevel} />
                    : <span className="text-tinta-suave">—</span>}
                </td>
                <td className="cifras py-2 pr-3">{fila.voteCount}</td>
                <td className="py-2 font-mono text-xs">{fila.origin}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
