"use client";

import { useState, type FormEvent } from "react";
import { enviarVoto } from "./api";
import type { Catalogo, Comprobante } from "./contrato";
import { mensajeDeRechazo } from "./rechazoVoto";
import { esUuid } from "./validacion";

export type ErroresVoto = Partial<Record<"voterId" | "region" | "candidateId", string>>;

// Estado del tarjetón: selección, validación local y envío del voto.
export function useVoto(catalogo: Catalogo, alAceptar: (c: Comprobante) => void) {
  const [voterId, setVoterId] = useState("");
  const [region, setRegion] = useState("");
  const [candidateId, setCandidateId] = useState("");
  const [errores, setErrores] = useState<ErroresVoto>({});
  const [rechazo, setRechazo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function quitarError(campo: keyof ErroresVoto) {
    setErrores((anteriores) => {
      if (anteriores[campo] === undefined) {
        return anteriores;
      }
      const restantes = { ...anteriores };
      delete restantes[campo];
      return restantes;
    });
  }

  function cambiarVoterId(valor: string) {
    setVoterId(valor);
    if (esUuid(valor)) {
      quitarError("voterId");
    }
  }

  function elegirRegion(valor: string) {
    setRegion(valor);
    quitarError("region");
  }

  function elegirCandidato(valor: string) {
    setCandidateId(valor);
    quitarError("candidateId");
  }

  async function depositar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (catalogo.candidates.length < 2) {
      setRechazo("La votación aún no está habilitada: se requieren al menos dos candidatos.");
      return;
    }
    const nuevos: ErroresVoto = {};
    if (!esUuid(voterId)) {
      nuevos.voterId = "Escriba el código completo: 36 caracteres con guiones.";
    }
    if (region === "") {
      nuevos.region = "Seleccione su región.";
    }
    if (candidateId === "") {
      nuevos.candidateId = "Marque un candidato.";
    }
    setErrores(nuevos);
    setRechazo(null);
    if (Object.keys(nuevos).length > 0) {
      return;
    }
    setEnviando(true);
    const resultado = await enviarVoto({ voterId: voterId.trim().toLowerCase(), candidateId, region });
    setEnviando(false);
    if (resultado.tipo === "aceptado") {
      alAceptar(resultado.comprobante);
      return;
    }
    setRechazo(mensajeDeRechazo(resultado));
  }

  return {
    voterId, region, candidateId, errores, rechazo, enviando,
    cambiarVoterId, elegirRegion, elegirCandidato, depositar,
  };
}
