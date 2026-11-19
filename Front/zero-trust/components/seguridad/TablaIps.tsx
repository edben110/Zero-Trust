import { fecha, type EstadoIp } from "@/lib/seguridad";
import { InsigniaNivel } from "./InsigniaNivel";

export function TablaIps({ ips }: { ips: EstadoIp[] }) {
  return (
    <>
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
                <td className="py-2 pr-3"><InsigniaNivel valor={fila.riskLevel} /></td>
                <td className="cifras py-2 pr-3">{fila.voteCount}</td>
                <td className="py-2 pr-3">{fecha(fila.firstSeen)}</td>
                <td className="py-2">{fecha(fila.lastSeen)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
