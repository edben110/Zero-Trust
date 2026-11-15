"use client";

import { useEffect, useState } from "react";
import { config } from "./config";
import type { EstadoConexion, ResultadosGlobales } from "./contrato";
import { simularResultados } from "./demo";
import { suscribir } from "./stomp";
import { leerResultadosGlobales, leerResultadosRegionales } from "./validacion";

export type EstadoResultados = {
  conexion: EstadoConexion;
  porDestino: Record<string, ResultadosGlobales>;
};

type Opciones = {
  url: string;
  destinos: string[];
  regional: boolean;
};

export function useResultados({ url, destinos, regional }: Opciones): EstadoResultados {
  const [conexion, setConexion] = useState<EstadoConexion>("conectando");
  const [porDestino, setPorDestino] = useState<Record<string, ResultadosGlobales>>({});
  const clave = destinos.join("|");

  useEffect(() => {
    const lista = clave === "" ? [] : clave.split("|");
    const guardar = (destino: string, resultados: ResultadosGlobales) =>
      setPorDestino((anteriores) => ({ ...anteriores, [destino]: resultados }));

    if (config.demo) {
      const detener = lista.map((destino, indice) =>
        simularResultados(indice + 1, (resultados) => {
          setConexion("en-vivo");
          guardar(destino, resultados);
        }),
      );
      return () => detener.forEach((parar) => parar());
    }

    return suscribir({
      url,
      destinos: lista,
      alCambiarEstado: setConexion,
      alRecibir: (destino, cuerpo) => {
        const resultados = regional ? leerResultadosRegionales(cuerpo) : leerResultadosGlobales(cuerpo);
        if (resultados !== null) {
          guardar(destino, resultados);
        }
      },
    });
  }, [url, clave, regional]);

  return { conexion, porDestino };
}
