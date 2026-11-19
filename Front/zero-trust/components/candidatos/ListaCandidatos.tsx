import type { Candidato, Catalogo } from "@/lib/contrato";

type Props = {
  catalogo: Catalogo | null;
  faltan: boolean;
  ocupado: boolean;
  onCerrar: () => void;
  onEditar: (c: Candidato) => void;
  onEliminar: (c: Candidato) => void;
};

export function ListaCandidatos({ catalogo, faltan, ocupado, onCerrar, onEditar, onEliminar }: Props) {
  return (
    <section aria-label="Listado de candidatos">
      <div className="flex items-center justify-between border-b-2 border-tinta pb-3">
        <h2 className="font-serif text-2xl">Candidatos ({catalogo?.candidates.length ?? "…" })</h2>
        <button onClick={onCerrar} className="text-sm underline underline-offset-4">Cerrar sesión</button>
      </div>
      {faltan && <p role="alert" className="mt-5 border-l-4 border-sello bg-papel-hondo p-4 font-medium">
        Solo hay un candidato. Cree al menos otro antes de salir para habilitar la votación.
      </p>}
      <ul className="divide-y divide-tinta/20">
        {catalogo?.candidates.map((c) => <li key={c.candidateId} className="flex flex-wrap items-center justify-between gap-3 py-5">
          <div><p className="font-serif text-xl">{c.name}</p><p className="text-sm text-tinta-suave">{c.list}</p></div>
          <div className="flex gap-3 text-sm">
            <button onClick={() => onEditar(c)} className="border border-tinta px-3 py-1 hover:bg-papel-hondo">Editar</button>
            <button disabled={ocupado || catalogo.candidates.length <= 2}
              title={catalogo.candidates.length <= 2 ? "Deben permanecer al menos dos candidatos" : undefined}
              onClick={() => onEliminar(c)}
              className="border border-sello px-3 py-1 text-sello disabled:opacity-40">Eliminar</button>
          </div>
        </li>)}
      </ul>
    </section>
  );
}
