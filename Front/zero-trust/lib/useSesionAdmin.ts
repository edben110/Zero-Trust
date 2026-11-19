"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";

export function useSesionAdmin(setAviso: (texto: string) => void) {
  const router = useRouter();
  const [autenticado, setAutenticado] = useState(false);
  const [cargando, setCargando] = useState(true);
  const [usuario, setUsuario] = useState("");
  const [password, setPassword] = useState("");
  const [ocupado, setOcupado] = useState(false);

  useEffect(() => {
    fetch("/admin/api/session", { cache: "no-store" })
      .then((res) => res.json())
      .then((datos) => setAutenticado(datos.autenticado === true))
      .catch(() => setAviso("No fue posible verificar la sesión."))
      .finally(() => setCargando(false));
  }, [setAviso]);

  async function iniciar(evento: FormEvent) {
    evento.preventDefault();
    setOcupado(true);
    setAviso("");
    try {
      const res = await fetch("/admin/api/session", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuario, password }),
      });
      if (!res.ok) { setAviso((await res.json()).error ?? "Acceso denegado."); return; }
      setPassword("");
      setAutenticado(true);
    } catch { setAviso("No fue posible iniciar sesión."); }
    finally { setOcupado(false); }
  }

  async function cerrar(alCerrar: () => void) {
    try {
      await fetch("/admin/api/session", { method: "DELETE", cache: "no-store" });
    } catch {
      // El estado local también se cierra si el servidor no responde.
    }
    setAutenticado(false);
    alCerrar();
    setUsuario("");
    setPassword("");
    router.refresh();
  }

  const perder = useCallback(() => setAutenticado(false), []);

  return { autenticado, cargando, usuario, setUsuario, password, setPassword, ocupado, iniciar, cerrar, perder };
}
