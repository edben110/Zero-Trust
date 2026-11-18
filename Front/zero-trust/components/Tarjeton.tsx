"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { enviarVoto } from "@/lib/api";
import type { Catalogo, Comprobante as DatosComprobante, ResultadoVoto } from "@/lib/contrato";
import { nombreRegion } from "@/lib/formato";
import { useCatalogo } from "@/lib/useCatalogo";
import { esUuid } from "@/lib/validacion";
import { Comprobante } from "./Comprobante";
import { MarcaX } from "./MarcaX";

type Errores = Partial<Record<"voterId" | "region" | "candidateId", string>>;

const MENSAJES_CAMPO: Record<string, string> = {
  voterId: "El código de votante no es válido.",
  candidateId: "El candidato marcado no está en el tarjetón.",
  region: "La región seleccionada no está en el tarjetón.",
};

function mensajeDeRechazo(resultado: Exclude<ResultadoVoto, { tipo: "aceptado" }>): string {
  switch (resultado.tipo) {
    case "invalido":
      return resultado.campos.length === 0
        ? "El voto no se pudo leer. Revise los datos e intente de nuevo."
        : resultado.campos.map((campo) => MENSAJES_CAMPO[campo] ?? `Revise el campo ${campo}.`).join(" ");
    case "no-habilitado":
      return "Este código no está habilitado para votar.";
    case "ya-voto":
      return "Ya se registró un voto para este documento en esta elección.";
    case "region-incorrecta":
      return "error esa no es la region que ud designo. Le queda un intento para seleccionar la región registrada.";
    case "anulado":
      return "Su voto quedó anulado por dos selecciones incorrectas de región. No puede votar en esta votación.";
    case "no-disponible":
      return "La validación del votante no está disponible en este momento. Su voto no se registró; intente de nuevo en unos minutos.";
    case "error":
      return "Ocurrió un error inesperado. Su voto no se registró.";
  }
}

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
  return <Formulario catalogo={catalogo.catalogo} alAceptar={setComprobante} />;
}

function Formulario({ catalogo, alAceptar }: { catalogo: Catalogo; alAceptar: (c: DatosComprobante) => void }) {
  const [voterId, setVoterId] = useState("");
  const [region, setRegion] = useState("");
  const [candidateId, setCandidateId] = useState("");
  const [errores, setErrores] = useState<Errores>({});
  const [rechazo, setRechazo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function quitarError(campo: keyof Errores) {
    setErrores((anteriores) => {
      if (anteriores[campo] === undefined) {
        return anteriores;
      }
      const restantes = { ...anteriores };
      delete restantes[campo];
      return restantes;
    });
  }

  async function depositar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (catalogo.candidates.length < 2) {
      setRechazo("La votación aún no está habilitada: se requieren al menos dos candidatos.");
      return;
    }
    const nuevos: Errores = {};
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

  return (
    <form noValidate onSubmit={depositar} className="grid gap-10">
      <fieldset>
        <legend className="font-serif text-xl">1. Su código de votante</legend>
        <label htmlFor="voterId" className="mt-3 block text-sm text-tinta-suave">
          Si no lo tiene, obténgalo con su cédula en{" "}
          <Link href="/mi-codigo" className="underline hover:text-tinta">
            Mi código de votante
          </Link>
          .
        </label>
        <input
          id="voterId"
          name="voterId"
          value={voterId}
          onChange={(evento) => {
            setVoterId(evento.target.value);
            if (esUuid(evento.target.value)) {
              quitarError("voterId");
            }
          }}
          autoComplete="off"
          spellCheck={false}
          aria-invalid={errores.voterId !== undefined}
          aria-describedby={errores.voterId ? "error-voterId" : undefined}
          placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
          className="mt-2 w-full border-b-2 border-tinta bg-transparent px-1 py-2 font-mono text-base placeholder:text-tinta/30 focus:bg-papel-hondo focus:outline-none"
        />
        {errores.voterId && (
          <p id="error-voterId" className="mt-2 text-sm text-sello">
            {errores.voterId}
          </p>
        )}
      </fieldset>

      <fieldset aria-describedby={errores.region ? "error-region" : undefined}>
        <legend className="font-serif text-xl">2. Su región</legend>
        <div className="mt-4 flex flex-wrap gap-2">
          {catalogo.regions.map((opcion) => (
            <label
              key={opcion}
              className="cursor-pointer border border-linea px-4 py-2 text-sm has-[:checked]:border-tinta has-[:checked]:bg-tinta has-[:checked]:text-papel has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-tinta"
            >
              <input
                type="radio"
                name="region"
                value={opcion}
                checked={region === opcion}
                onChange={() => {
                  setRegion(opcion);
                  quitarError("region");
                }}
                className="sr-only"
              />
              {nombreRegion(opcion)}
            </label>
          ))}
        </div>
        {errores.region && (
          <p id="error-region" className="mt-2 text-sm text-sello">
            {errores.region}
          </p>
        )}
      </fieldset>

      <fieldset aria-describedby={errores.candidateId ? "error-candidato" : undefined}>
        <legend className="font-serif text-xl">3. Marque un candidato</legend>
        <p className="mt-2 text-sm text-tinta-suave">Marque una sola casilla.</p>
        <div className="mt-4 grid gap-px border border-linea bg-linea sm:grid-cols-2">
          {catalogo.candidates.map((candidato, indice) => {
            const marcado = candidateId === candidato.candidateId;
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
                  onChange={() => {
                    setCandidateId(candidato.candidateId);
                    quitarError("candidateId");
                  }}
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
        {errores.candidateId && (
          <p id="error-candidato" className="mt-2 text-sm text-sello">
            {errores.candidateId}
          </p>
        )}
      </fieldset>

      <div className="grid gap-4">
        {catalogo.candidates.length < 2 && <p role="status" className="border-l-4 border-sello bg-papel-hondo px-4 py-3">
          La votación aún no está habilitada: se requieren al menos dos candidatos.
        </p>}
        <div aria-live="assertive">
          {rechazo && (
            <p role="alert" className="border-l-4 border-sello bg-papel-hondo px-4 py-3 text-sm">
              {rechazo}
            </p>
          )}
        </div>
        <button
          type="submit"
          disabled={enviando || catalogo.candidates.length < 2}
          className="justify-self-start border-2 border-tinta bg-tinta px-6 py-3 font-medium text-papel hover:bg-papel hover:text-tinta disabled:cursor-wait disabled:opacity-60"
        >
          {enviando ? "Enviando su voto…" : "Depositar voto"}
        </button>
      </div>
    </form>
  );
}
