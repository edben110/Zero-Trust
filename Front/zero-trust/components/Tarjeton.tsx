"use client";

import { useState } from "react";
import type { Comprobante as DatosComprobante } from "@/lib/contrato";
import { useCatalogo } from "@/lib/useCatalogo";
import { Comprobante } from "./Comprobante";
import { FormularioVoto } from "./tarjeton/FormularioVoto";

export function Tarjeton() {
  const { catalogo, reintentar } = useCatalogo();
  const [comprobante, setComprobante] = useState<DatosComprobante | null>(null);

  if (comprobante !== null) {
    return <Comprobante comprobante={comprobante} alVolver={() => setComprobante(null)} />;
  }
  if (catalogo.estado === "cargando") {
    return <p className="py-16 text-center text-tinta-suave">Cargando el tarjetón…</p>;
  }
  if (catalogo.estado === "error") {
    return (
      <div role="alert" className="mx-auto max-w-xl border border-sello px-6 py-6">
        <p className="font-semibold text-sello">No fue posible cargar el tarjetón.</p>
        <p className="mt-2 text-sm">Verifique su conexión e intente de nuevo.</p>
        <button
          type="button"
          onClick={reintentar}
          className="mt-4 border border-tinta px-4 py-2 text-sm font-medium hover:bg-tinta hover:text-papel"
        >
          Intentar de nuevo
        </button>
      </div>
    );
  }
  return <FormularioVoto catalogo={catalogo.catalogo} alAceptar={setComprobante} />;
}
