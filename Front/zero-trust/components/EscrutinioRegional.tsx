"use client";

import { config } from "@/lib/config";
import { nombreRegion } from "@/lib/formato";
import { useCatalogo } from "@/lib/useCatalogo";
import { useResultados } from "@/lib/useResultados";
import { Acta } from "./Acta";
import { EstadoConexion } from "./EstadoConexion";

function destino(region: string): string {
  return `/topic/region/${region}`;
}

export function EscrutinioRegional() {
  const { catalogo, reintentar } = useCatalogo();
  const regiones = catalogo.estado === "listo" ? catalogo.catalogo.regions : [];
  const { conexion, porDestino } = useResultados({
    url: config.wsRegionalUrl,
    destinos: regiones.map(destino),
    regional: true,
  });

  return (
    <section aria-labelledby="titulo-regiones">
      <header className="flex flex-wrap items-end justify-between gap-x-6 gap-y-2 border-b-2 border-tinta pb-3">
        <h2 id="titulo-regiones" className="font-serif text-3xl sm:text-4xl">
          Escrutinio por región
        </h2>
        <EstadoConexion estado={catalogo.estado === "listo" ? conexion : "conectando"} />
      </header>

      {catalogo.estado === "cargando" && <p className="py-10 text-center text-tinta-suave">Cargando las regiones…</p>}

      {catalogo.estado === "error" && (
        <div role="alert" className="mt-6 border border-sello px-6 py-6">
          <p className="font-semibold text-sello">No fue posible cargar las regiones.</p>
          <button
            type="button"
            onClick={reintentar}
            className="mt-4 border border-tinta px-4 py-2 text-sm font-medium hover:bg-tinta hover:text-papel"
          >
            Intentar de nuevo
          </button>
        </div>
      )}

      {catalogo.estado === "listo" && (
        <div className="mt-6 grid gap-6 md:grid-cols-2">
          {regiones.map((region) => (
            <Acta
              key={region}
              titulo={nombreRegion(region)}
              resultados={porDestino[destino(region)]}
              catalogo={catalogo.catalogo}
              conexion={conexion}
              compacta
            />
          ))}
        </div>
      )}
    </section>
  );
}
