import type { Catalogo, EstadoConexion as Estado, ResultadosGlobales } from "@/lib/contrato";
import { formatearHora, formatearNumero, formatearPorcentaje } from "@/lib/formato";
import { EstadoConexion } from "./EstadoConexion";

type Props = {
  titulo: string;
  resultados: ResultadosGlobales | undefined;
  catalogo: Catalogo | null;
  conexion: Estado;
  compacta?: boolean;
};

// Los workers envían candidateName igual al candidateId: el nombre se resuelve con el catálogo.
function nombres(catalogo: Catalogo | null): Map<string, { nombre: string; lista: string }> {
  return new Map((catalogo?.candidates ?? []).map((c) => [c.candidateId, { nombre: c.name, lista: c.list }]));
}

export function Acta({ titulo, resultados, catalogo, conexion, compacta = false }: Props) {
  const filas = [...(resultados?.results ?? [])].sort((a, b) => b.voteCount - a.voteCount);
  const total = filas.reduce((suma, fila) => suma + fila.voteCount, 0);
  const porId = nombres(catalogo);
  const Titulo = compacta ? "h3" : "h2";

  return (
    <section aria-label={titulo} className={compacta ? "border border-linea p-4" : ""}>
      <header className="flex flex-wrap items-end justify-between gap-x-6 gap-y-2 border-b-2 border-tinta pb-3">
        <Titulo className={compacta ? "font-serif text-xl" : "font-serif text-3xl sm:text-4xl"}>{titulo}</Titulo>
        <EstadoConexion estado={conexion} />
      </header>

      <dl className="mt-3 flex flex-wrap gap-x-8 gap-y-1 text-sm">
        <div className="flex gap-2">
          <dt className="text-tinta-suave">Total escrutado</dt>
          <dd className="cifras font-semibold">{formatearNumero(total)} votos</dd>
        </div>
        <div className="flex gap-2">
          <dt className="text-tinta-suave">Última actualización</dt>
          <dd className="cifras font-mono">{resultados ? formatearHora(resultados.timestamp) : "—"}</dd>
        </div>
      </dl>

      {total === 0 ? (
        <p className="py-10 text-center text-tinta-suave">Aún no hay votos escrutados.</p>
      ) : (
        <table className="mt-4 w-full border-collapse text-left" aria-live="polite">
          <caption className="sr-only">
            {titulo}: votos y porcentaje por candidato
          </caption>
          <thead>
            <tr className="border-b border-linea text-xs uppercase tracking-wider text-tinta-suave">
              <th scope="col" className="py-2 pr-3 font-medium">Candidato</th>
              <th scope="col" className="py-2 pl-3 text-right font-medium">Votos</th>
              <th scope="col" className="py-2 pl-3 text-right font-medium">%</th>
            </tr>
          </thead>
          <tbody>
            {filas.map((fila) => {
              const datos = porId.get(fila.candidateId);
              const proporcion = total === 0 ? 0 : (fila.voteCount / total) * 100;
              return (
                <tr key={fila.candidateId} className="border-b border-linea align-top">
                  <th scope="row" className="py-3 pr-3 font-normal">
                    <span className={compacta ? "block text-sm" : "block font-serif text-lg leading-snug"}>
                      {datos?.nombre ?? `Candidato ${fila.candidateId.slice(0, 8)}`}
                    </span>
                    {datos && !compacta && <span className="block text-sm text-tinta-suave">{datos.lista}</span>}
                    <span className="mt-2 block h-1.5 bg-linea" aria-hidden="true">
                      <span className="barra block h-full bg-tinta" style={{ width: `${proporcion}%` }} />
                    </span>
                  </th>
                  <td className="cifras py-3 pl-3 text-right font-mono">
                    <span key={fila.voteCount} className="cifra-viva px-1">
                      {formatearNumero(fila.voteCount)}
                    </span>
                  </td>
                  <td className="cifras py-3 pl-3 text-right font-mono">{formatearPorcentaje(fila.voteCount, total)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}
