import type { Comprobante as DatosComprobante } from "@/lib/contrato";
import { formatearFecha, formatearHora } from "@/lib/formato";

const RECORRIDO = ["Datos del voto validados", "Votante habilitado", "Voto registrado"] as const;

type Props = {
  comprobante: DatosComprobante;
  alVolver: () => void;
};

export function Comprobante({ comprobante, alVolver }: Props) {
  return (
    <section aria-labelledby="titulo-comprobante" className="mx-auto max-w-xl">
      <div className="perforado px-6 py-8 sm:px-10">
        <div className="flex flex-col items-start gap-4 sm:flex-row sm:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-tinta-suave">Comprobante de voto</p>
            <h2 id="titulo-comprobante" className="mt-1 font-serif text-3xl">
              Su voto fue recibido
            </h2>
          </div>
          <p className="sello shrink-0 px-3 py-1 font-mono text-sm font-semibold" aria-hidden="true">
            RECIBIDO
          </p>
        </div>

        <dl className="mt-8 grid gap-4 text-sm">
          <div>
            <dt className="text-tinta-suave">Código del voto</dt>
            <dd className="mt-1 font-mono text-base break-all">{comprobante.voteId}</dd>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <dt className="text-tinta-suave">Fecha</dt>
              <dd className="mt-1">{formatearFecha(comprobante.receivedAt)}</dd>
            </div>
            <div>
              <dt className="text-tinta-suave">Hora (Bogotá)</dt>
              <dd className="cifras mt-1 font-mono">{formatearHora(comprobante.receivedAt)}</dd>
            </div>
          </div>
        </dl>

        <div className="linea-corte mt-8 pt-6">
          <h3 className="text-xs uppercase tracking-[0.2em] text-tinta-suave">Recorrido del voto</h3>
          <ol className="mt-3 grid gap-2 text-sm">
            {RECORRIDO.map((paso, indice) => (
              <li key={paso} className="flex items-baseline gap-3">
                <span className="cifras font-mono text-tinta-suave">{String(indice + 1).padStart(2, "0")}</span>
                <span>{paso}</span>
                <span className="ml-auto text-xs font-semibold uppercase tracking-wider text-verificado">Verificado</span>
              </li>
            ))}
          </ol>
        </div>
      </div>

      <p className="mt-6 text-sm text-tinta-suave">
        Guarde el código del voto: con él puede ubicarse su voto en el registro de auditoría.
      </p>
      <button
        type="button"
        onClick={alVolver}
        className="mt-4 border border-tinta px-5 py-2.5 text-sm font-medium hover:bg-tinta hover:text-papel"
      >
        Volver al tarjetón
      </button>
    </section>
  );
}
