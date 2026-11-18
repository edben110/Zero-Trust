"use client";

import { useState, type FormEvent } from "react";
import { nombreRegion } from "@/lib/formato";
import { normalizarCedula } from "@/lib/validacion";

const campo = "w-full border border-tinta bg-papel px-3 py-2 outline-offset-2 focus:outline focus:outline-sello";
const boton = "border-2 border-tinta bg-tinta px-5 py-2 font-medium text-papel hover:bg-papel hover:text-tinta disabled:opacity-50";

const MENSAJES_CAMPO: Record<string, string> = {
  cedula: "La cédula no es válida (de 6 a 10 dígitos).",
  region: "Seleccione una región del tarjetón.",
};

function mensajeDeError(estado: number, datos: unknown): string {
  const cuerpo = typeof datos === "object" && datos !== null ? (datos as Record<string, unknown>) : {};
  if (estado === 400 && Array.isArray(cuerpo.fields) && cuerpo.fields.length > 0) {
    return cuerpo.fields.map((campo) => MENSAJES_CAMPO[String(campo)] ?? `Revise el campo ${String(campo)}.`).join(" ");
  }
  if (estado === 409) return "Este documento ya está inscrito en el padrón de esta elección.";
  if (estado === 401 || estado === 403) return "La sesión expiró. Vuelva a ingresar.";
  if (estado === 503) return "El servicio no está disponible. No se inscribió al votante; intente de nuevo.";
  return typeof cuerpo.error === "string" ? cuerpo.error : "No se pudo inscribir al votante.";
}

// Alta de votantes en el padrón. La cédula no se guarda: el servidor solo guarda el código derivado de ella.
export function AdministracionPadron({ regiones }: { regiones: string[] }) {
  const [cedula, setCedula] = useState("");
  const [region, setRegion] = useState("");
  const [aviso, setAviso] = useState<{ texto: string; error: boolean } | null>(null);
  const [ocupado, setOcupado] = useState(false);

  async function inscribir(evento: FormEvent) {
    evento.preventDefault();
    const normalizada = normalizarCedula(cedula);
    if (normalizada === null) {
      setAviso({ texto: MENSAJES_CAMPO.cedula, error: true });
      return;
    }
    if (region === "") {
      setAviso({ texto: "Seleccione una región.", error: true });
      return;
    }
    setOcupado(true);
    setAviso(null);
    try {
      const res = await fetch("/admin/api/voters", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ cedula: normalizada, region }),
      });
      const datos = await res.json().catch(() => null);
      if (!res.ok) {
        setAviso({ texto: mensajeDeError(res.status, datos), error: true });
        return;
      }
      setCedula("");
      setRegion("");
      setAviso({ texto: "Votante inscrito. Ya puede consultar su código en «Mi código de votante».", error: false });
    } catch {
      setAviso({ texto: "No se pudo comunicar con el servidor.", error: true });
    } finally {
      setOcupado(false);
    }
  }

  return (
    <section aria-labelledby="titulo-padron" className="mt-12 border-t-2 border-tinta pt-8">
      <h2 id="titulo-padron" className="font-serif text-2xl">Padrón electoral</h2>
      <p className="mt-2 max-w-2xl text-sm text-tinta-suave">
        Inscriba a quien no esté en el padrón. Después podrá obtener su código de votante con su cédula y votar una vez.
        La cédula no se guarda.
      </p>
      <form noValidate onSubmit={inscribir} className="mt-6 grid max-w-xl gap-5 border border-tinta p-6">
        <label className="block text-sm font-medium">
          Número de cédula
          <input
            className={`${campo} mt-2 font-mono`}
            inputMode="numeric"
            autoComplete="off"
            autoCorrect="off"
            spellCheck={false}
            value={cedula}
            onChange={(e) => setCedula(e.target.value)}
            placeholder="1234567890"
            required
          />
        </label>
        <label className="block text-sm font-medium">
          Región
          <select className={`${campo} mt-2`} value={region} onChange={(e) => setRegion(e.target.value)} required>
            <option value="">Seleccione…</option>
            {regiones.map((r) => (
              <option key={r} value={r}>{nombreRegion(r)}</option>
            ))}
          </select>
        </label>
        <button className={`${boton} justify-self-start`} disabled={ocupado}>
          {ocupado ? "Inscribiendo…" : "Inscribir en el padrón"}
        </button>
        <div aria-live="polite">
          {aviso && (
            <p role={aviso.error ? "alert" : "status"} className={aviso.error ? "text-sello" : "font-medium"}>
              {aviso.texto}
            </p>
          )}
        </div>
      </form>
    </section>
  );
}
