"use client";

import { config } from "@/lib/config";
import { useCatalogo } from "@/lib/useCatalogo";
import { useResultados } from "@/lib/useResultados";
import { Acta } from "./Acta";

const DESTINOS = ["/topic/global"];

export function EscrutinioNacional() {
  const { catalogo } = useCatalogo();
  const { conexion, porDestino } = useResultados({ url: config.wsGlobalUrl, destinos: DESTINOS, regional: false });

  return (
    <>
      {catalogo.estado === "error" && (
        <p role="alert" className="mb-6 border-l-4 border-sello bg-papel-hondo px-4 py-3 text-sm">
          No fue posible cargar los nombres de los candidatos; se muestran sus códigos.
        </p>
      )}
      <Acta
        titulo="Escrutinio nacional"
        resultados={porDestino["/topic/global"]}
        catalogo={catalogo.estado === "listo" ? catalogo.catalogo : null}
        conexion={conexion}
      />
    </>
  );
}
