"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { consultarCodigo } from "@/lib/api";
import type { ResultadoConsulta } from "@/lib/contrato";
import { normalizarCedula } from "@/lib/validacion";

function mensajeDeFallo(resultado: Exclude<ResultadoConsulta, { tipo: "encontrado" }>): string {
  switch (resultado.tipo) {
    case "invalido":
      return "El número de cédula no es válido. Escriba de 6 a 10 dígitos.";
    case "no-registrado":
      return "El titular de este documento no se encuentra habilitado en el padrón. Por favor pídale al administrador que lo añada.";
    case "no-disponible":
      return "El servicio no está disponible en este momento. Intente de nuevo en unos minutos.";
    case "error":
      return "Ocurrió un error inesperado. Intente de nuevo.";
  }
}

export function ConsultaCodigo() {
  const [cedula, setCedula] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [codigo, setCodigo] = useState<string | null>(null);
  const [copiado, setCopiado] = useState(false);
  const [consultando, setConsultando] = useState(false);

  async function consultar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setCodigo(null);
    setCopiado(false);
    const normalizada = normalizarCedula(cedula);
    if (normalizada === null) {
      setError("Escriba su número de cédula: de 6 a 10 dígitos.");
      return;
    }
    setError(null);
    setConsultando(true);
    const resultado = await consultarCodigo(normalizada);
    setConsultando(false);
    if (resultado.tipo === "encontrado") {
      setCedula("");   // la cédula no se conserva en pantalla después de la consulta
      setCodigo(resultado.voterId);
      return;
    }
    if (resultado.tipo === "no-registrado") {
      setCedula("");
    }
    setError(mensajeDeFallo(resultado));
  }

  async function copiar() {
    if (codigo === null) {
      return;
    }
    try {
      await navigator.clipboard.writeText(codigo);
      setCopiado(true);
    } catch {
      setCopiado(false);
    }
  }

  return (
    <div className="grid max-w-xl gap-8">
      <form noValidate onSubmit={consultar} className="grid gap-4">
        <label htmlFor="cedula" className="font-serif text-xl">
          Número de cédula
        </label>
        <input
          id="cedula"
          name="cedula"
          type="text"
          inputMode="numeric"
          value={cedula}
          onChange={(evento) => {
            setCedula(evento.target.value);
            setError(null);
          }}
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          aria-invalid={error !== null}
          aria-describedby={error ? "error-cedula" : undefined}
          placeholder="1234567890"
          className="w-full border-b-2 border-tinta bg-transparent px-1 py-2 font-mono text-base placeholder:text-tinta/30 focus:bg-papel-hondo focus:outline-none"
        />
        <div aria-live="assertive">
          {error && (
            <p id="error-cedula" role="alert" className="text-sm text-sello">
              {error}
            </p>
          )}
        </div>
        <button
          type="submit"
          disabled={consultando}
          className="justify-self-start border-2 border-tinta bg-tinta px-6 py-3 font-medium text-papel hover:bg-papel hover:text-tinta disabled:cursor-wait disabled:opacity-60"
        >
          {consultando ? "Consultando…" : "Obtener mi código"}
        </button>
      </form>

      {codigo && (
        <section aria-labelledby="titulo-codigo" className="perforado px-6 py-6">
          <h2 id="titulo-codigo" className="text-xs uppercase tracking-[0.2em] text-tinta-suave">
            Su código de votante
          </h2>
          <p className="mt-2 font-mono text-lg break-all">{codigo}</p>
          <p className="mt-3 text-sm text-tinta-suave">
            Con este código vota una sola vez en esta elección. No lo comparta.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={copiar}
              className="border border-tinta px-4 py-2 text-sm font-medium hover:bg-tinta hover:text-papel"
            >
              {copiado ? "Copiado" : "Copiar código"}
            </button>
            <Link
              href="/"
              className="border-2 border-tinta bg-tinta px-4 py-2 text-sm font-medium text-papel hover:bg-papel hover:text-tinta"
            >
              Ir a votar
            </Link>
          </div>
          <p aria-live="polite" className="sr-only">
            {copiado ? "Código copiado al portapapeles" : ""}
          </p>
        </section>
      )}
    </div>
  );
}
