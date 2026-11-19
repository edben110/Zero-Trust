"use client";

import { useCallback, useEffect, useState } from "react";
import { leerDatos, type Aviso, type DatosSeguridad } from "./seguridad";

export function useSeguridad() {
  const [datos, setDatos] = useState<DatosSeguridad | null>(null);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [actualizadoEn, setActualizadoEn] = useState<string | null>(null);

  const descargar = useCallback(async (): Promise<DatosSeguridad | null> => {
    try {
      const res = await fetch("/admin/api/security/incidents", { cache: "no-store" });
      if (res.status === 401 || res.status === 403) return null;
      if (!res.ok) return null;
      setActualizadoEn(new Date().toLocaleTimeString("es-CO"));
      return leerDatos(await res.json());
    } catch {
      // El refresco automático es best-effort; el aviso lo da una acción explícita.
      return null;
    }
  }, []);

  const cargar = useCallback(async () => {
    const leidos = await descargar();
    if (leidos) setDatos(leidos);
  }, [descargar]);

  useEffect(() => {
    // Asíncrono: el estado solo cambia cuando llega la respuesta, nunca durante el efecto.
    let cancelado = false;
    const refrescar = async () => {
      const leidos = await descargar();
      if (!cancelado && leidos) setDatos(leidos);
    };
    void refrescar();
    const intervalo = window.setInterval(() => { void refrescar(); }, 3000);
    return () => { cancelado = true; window.clearInterval(intervalo); };
  }, [descargar]);

  async function llamar(ruta: string, cuerpo: Record<string, unknown>, exito: string) {
    setOcupado(true);
    setAviso(null);
    try {
      const res = await fetch(ruta, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cuerpo),
      });
      const respuesta = await res.json().catch(() => null);
      if (!res.ok) {
        setAviso({ texto: respuesta?.error ?? "No se pudo completar la operación.", error: true });
        return;
      }
      setAviso({ texto: exito, error: false });
      // El detector aplica la prueba de forma asíncrona: se da un margen antes de releer el estado.
      await new Promise((listo) => window.setTimeout(listo, 800));
      await cargar();
    } catch {
      setAviso({ texto: "No se pudo comunicar con el servidor.", error: true });
    } finally {
      setOcupado(false);
    }
  }

  // El sondeo ya refresca la vista; esto fuerza la lectura y deja constancia de que se consultó.
  async function actualizar() {
    setOcupado(true);
    setAviso(null);
    const leidos = await descargar();
    if (leidos) {
      setDatos(leidos);
      setAviso({ texto: `Estado actualizado a las ${new Date().toLocaleTimeString("es-CO")}.`, error: false });
    } else {
      setAviso({ texto: "No se pudo actualizar. Verifique la sesión y vuelva a iniciar sesión.", error: true });
    }
    setOcupado(false);
  }

  async function purgar() {
    if (!window.confirm("Se eliminarán TODOS los incidentes y los estados por IP. Los contadores quedarán en cero y la acción no se puede deshacer. ¿Continuar?")) return;
    await llamar("/admin/api/security/purge", {},
      "Incidentes y estados por IP eliminados. Los contadores quedaron en cero.");
  }

  return { datos, aviso, setAviso, ocupado, actualizadoEn, llamar, actualizar, purgar };
}
