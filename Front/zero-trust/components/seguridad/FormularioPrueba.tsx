"use client";

import { useState, type FormEvent } from "react";
import type { Aviso } from "@/lib/seguridad";

const campo = "w-full border border-tinta bg-papel px-3 py-2 outline-offset-2 focus:outline focus:outline-sello";
const boton = "border-2 border-tinta bg-tinta px-5 py-2 font-medium text-papel hover:bg-papel hover:text-tinta disabled:opacity-50";

type Props = {
  ocupado: boolean;
  aviso: Aviso | null;
  onAviso: (aviso: Aviso) => void;
  llamar: (ruta: string, cuerpo: Record<string, unknown>, exito: string) => Promise<void>;
};

export function FormularioPrueba({ ocupado, aviso, onAviso, llamar }: Props) {
  const [ip, setIp] = useState("");
  const [votos, setVotos] = useState("12");

  async function simular(evento: FormEvent) {
    evento.preventDefault();
    const cantidad = Number(votos);
    if (!Number.isInteger(cantidad) || cantidad < 1 || cantidad > 50) {
      onAviso({ texto: "El número de votos debe estar entre 1 y 50.", error: true });
      return;
    }
    await llamar("/admin/api/security/drills", { ip: ip.trim(), votes: cantidad },
      `Prueba enviada: ${cantidad} voto(s) desde ${ip.trim()}. El estado se actualizará en unos segundos.`);
  }

  async function reiniciar() {
    if (!window.confirm(`¿Devolver ${ip.trim() || "la IP"} a NORMAL? Se descarta su estado en memoria; el histórico de incidentes se conserva.`)) return;
    await llamar("/admin/api/security/reset", { ip: ip.trim() },
      `Reinicio enviado para ${ip.trim()}. La IP vuelve a NORMAL y sus contadores a cero.`);
  }

  return (
    <form onSubmit={simular} className="mt-8 grid gap-5 border border-tinta p-6 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <label className="block text-sm font-medium">Dirección IP real o de prueba
        <input className={`${campo} mt-2 font-mono`} value={ip} onChange={(e) => setIp(e.target.value)}
          placeholder="203.0.113.10" autoComplete="off" spellCheck={false} required />
      </label>
      <label className="block text-sm font-medium">Votos simulados
        <input className={`${campo} mt-2 cifras`} type="number" min={1} max={50} value={votos}
          onChange={(e) => setVotos(e.target.value)} required />
      </label>
      <div className="flex flex-wrap items-center gap-4 sm:col-span-2">
        <button className={boton} disabled={ocupado}>{ocupado ? "Enviando…" : "Ejecutar prueba"}</button>
        <button type="button" onClick={() => void reiniciar()} disabled={ocupado}
          className="border-2 border-sello px-5 py-2 font-medium text-sello disabled:opacity-50">
          Restablecer IP
        </button>
      </div>
      <div aria-live="polite" className="sm:col-span-2">
        {aviso && <p role={aviso.error ? "alert" : "status"} className={aviso.error ? "text-sello" : "font-medium"}>{aviso.texto}</p>}
      </div>
    </form>
  );
}
