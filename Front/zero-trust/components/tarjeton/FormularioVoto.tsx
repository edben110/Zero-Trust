"use client";

import type { Catalogo, Comprobante } from "@/lib/contrato";
import { useVoto } from "@/lib/useVoto";
import { CampoCodigo } from "./CampoCodigo";
import { EnvioVoto } from "./EnvioVoto";
import { SelectorCandidato } from "./SelectorCandidato";
import { SelectorRegion } from "./SelectorRegion";

type Props = { catalogo: Catalogo; alAceptar: (c: Comprobante) => void };

export function FormularioVoto({ catalogo, alAceptar }: Props) {
  const voto = useVoto(catalogo, alAceptar);

  return (
    <form noValidate onSubmit={voto.depositar} className="grid gap-10">
      <CampoCodigo valor={voto.voterId} error={voto.errores.voterId} alCambiar={voto.cambiarVoterId} />
      <SelectorRegion regiones={catalogo.regions} valor={voto.region} error={voto.errores.region}
        alElegir={voto.elegirRegion} />
      <SelectorCandidato candidatos={catalogo.candidates} valor={voto.candidateId} error={voto.errores.candidateId}
        alElegir={voto.elegirCandidato} />
      <EnvioVoto habilitada={catalogo.candidates.length >= 2} rechazo={voto.rechazo} enviando={voto.enviando} />
    </form>
  );
}
