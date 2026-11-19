import type { Candidato } from "@/lib/contrato";
import { MarcaX } from "../MarcaX";

type Props = { candidatos: Candidato[]; valor: string; error?: string; alElegir: (candidateId: string) => void };

export function SelectorCandidato({ candidatos, valor, error, alElegir }: Props) {
  return (
    <fieldset aria-describedby={error ? "error-candidato" : undefined}>
      <legend className="font-serif text-xl">3. Marque un candidato</legend>
      <p className="mt-2 text-sm text-tinta-suave">Marque una sola casilla.</p>
      <div className="mt-4 grid gap-px border border-linea bg-linea sm:grid-cols-2">
        {candidatos.map((candidato, indice) => {
          const marcado = valor === candidato.candidateId;
          return (
            <label
              key={candidato.candidateId}
              className="flex cursor-pointer items-center gap-4 bg-papel px-4 py-4 hover:bg-papel-hondo has-[:focus-visible]:outline-2 has-[:focus-visible]:-outline-offset-2 has-[:focus-visible]:outline-tinta"
            >
              <input
                type="radio"
                name="candidato"
                value={candidato.candidateId}
                checked={marcado}
                onChange={() => alElegir(candidato.candidateId)}
                className="sr-only"
              />
              <span className="relative flex h-14 w-14 shrink-0 items-center justify-center border-2 border-tinta">
                <span className="cifras font-mono text-xs text-tinta-suave">{String(indice + 1).padStart(2, "0")}</span>
                {marcado && <MarcaX />}
              </span>
              <span>
                <span className="block font-serif text-lg leading-snug">{candidato.name}</span>
                <span className="block text-sm text-tinta-suave">{candidato.list}</span>
              </span>
            </label>
          );
        })}
      </div>
      {error && (
        <p id="error-candidato" className="mt-2 text-sm text-sello">
          {error}
        </p>
      )}
    </fieldset>
  );
}
