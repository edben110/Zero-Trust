"use client";

import { useCallback, useEffect, useState } from "react";
import type { Candidato, Catalogo } from "./contrato";
import { leerCatalogo } from "./validacion";

export function useCatalogoAdmin(autenticado: boolean, perderSesion: () => void, setAviso: (texto: string) => void) {
  const [catalogo, setCatalogo] = useState<Catalogo | null>(null);
  const [nombre, setNombre] = useState("");
  const [lista, setLista] = useState("");
  const [editando, setEditando] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const cargar = useCallback(async () => {
    const res = await fetch("/admin/api/candidates", { cache: "no-store" });
    if (res.status === 401 || res.status === 403) {
      perderSesion();
      setCatalogo(null);
      setAviso("La sesión terminó o fue reemplazada por el acceso desde otro dispositivo. Ingrese nuevamente.");
      return;
    }
    const datos = res.ok ? leerCatalogo(await res.json()) : null;
    if (datos) setCatalogo(datos);
    else setAviso("No fue posible cargar el catálogo.");
  }, [perderSesion, setAviso]);

  useEffect(() => {
    if (!autenticado) return;
    // Primera lectura en cuanto hay sesión; asíncrona para no cambiar estado durante el efecto.
    void Promise.resolve().then(cargar);
    const intervalo = window.setInterval(() => { void cargar(); }, 2000);
    return () => window.clearInterval(intervalo);
  }, [autenticado, cargar]);

  async function mutar(metodo: "POST" | "PUT" | "DELETE", id?: string) {
    setOcupado(true);
    setAviso("");
    try {
      const res = await fetch(`/admin/api/candidates${id ? `/${id}` : ""}`, {
        method: metodo,
        headers: { "Content-Type": "application/json" },
        body: metodo === "DELETE" ? undefined : JSON.stringify({ name: nombre.trim(), list: lista.trim() }),
      });
      const datos = await res.json();
      if (!res.ok) { setAviso(datos.error ?? "No se pudo guardar el cambio."); return; }
      const nuevo = leerCatalogo(datos);
      if (!nuevo) throw new Error("Respuesta de catálogo inválida");
      setCatalogo(nuevo);
      setNombre(""); setLista(""); setEditando(null);
      setAviso("Catálogo actualizado.");
      window.dispatchEvent(new Event("catalogo-actualizado"));
    } catch { setAviso("No se pudo comunicar con el servidor."); }
    finally { setOcupado(false); }
  }

  async function reiniciarVotacion() {
    if (!window.confirm("¿Iniciar otra votación con los mismos candidatos? Los votos actuales dejarán de contarse y cada votante podrá votar de nuevo. Esta acción no se puede deshacer.")) return;
    setOcupado(true);
    setAviso("");
    try {
      const respuesta = await fetch("/admin/api/election/reset", { method: "POST" });
      const datos = await respuesta.json();
      if (!respuesta.ok) { setAviso(datos.error ?? "No se pudo reiniciar la votación."); return; }
      setAviso("Nueva votación iniciada. Los candidatos se conservaron y los resultados comienzan en cero.");
    } catch { setAviso("No se pudo comunicar con el servidor."); }
    finally { setOcupado(false); }
  }

  function editar(c: Candidato) {
    setEditando(c.candidateId);
    setNombre(c.name);
    setLista(c.list);
  }

  function cancelar() {
    setEditando(null);
    setNombre("");
    setLista("");
  }

  const faltan = catalogo !== null && catalogo.candidates.length < 2;

  return {
    catalogo, limpiar: () => setCatalogo(null), faltan, ocupado,
    nombre, setNombre, lista, setLista, editando, editar, cancelar, mutar, reiniciarVotacion,
  };
}
