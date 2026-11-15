"use client";

import { useCallback, useEffect, useState } from "react";
import { obtenerCatalogo } from "./api";
import type { Catalogo } from "./contrato";

export type EstadoCatalogo =
  | { estado: "cargando" }
  | { estado: "listo"; catalogo: Catalogo }
  | { estado: "error" };

export function useCatalogo(): { catalogo: EstadoCatalogo; reintentar: () => void } {
  const [catalogo, setCatalogo] = useState<EstadoCatalogo>({ estado: "cargando" });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    obtenerCatalogo().then(
      (datos) => vigente && setCatalogo({ estado: "listo", catalogo: datos }),
      () => vigente && setCatalogo({ estado: "error" }),
    );
    return () => {
      vigente = false;
    };
  }, [intento]);

  const reintentar = useCallback(() => {
    setCatalogo({ estado: "cargando" });
    setIntento((anterior) => anterior + 1);
  }, []);

  return { catalogo, reintentar };
}
