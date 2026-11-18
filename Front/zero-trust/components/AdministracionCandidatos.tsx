"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import type { Candidato, Catalogo } from "@/lib/contrato";
import { leerCatalogo } from "@/lib/validacion";
import { AdministracionPadron } from "./AdministracionPadron";

const campo = "w-full border border-tinta bg-papel px-3 py-2 outline-offset-2 focus:outline focus:outline-sello";
const boton = "border-2 border-tinta bg-tinta px-5 py-2 font-medium text-papel hover:bg-papel hover:text-tinta disabled:opacity-50";

export function AdministracionCandidatos() {
  const router = useRouter();
  const [autenticado, setAutenticado] = useState(false);
  const [cargando, setCargando] = useState(true);
  const [usuario, setUsuario] = useState("");
  const [password, setPassword] = useState("");
  const [catalogo, setCatalogo] = useState<Catalogo | null>(null);
  const [nombre, setNombre] = useState("");
  const [lista, setLista] = useState("");
  const [editando, setEditando] = useState<string | null>(null);
  const [aviso, setAviso] = useState("");
  const [ocupado, setOcupado] = useState(false);

  const cargar = useCallback(async () => {
    const res = await fetch("/admin/api/candidates", { cache: "no-store" });
    if (res.status === 401 || res.status === 403) {
      setAutenticado(false);
      setCatalogo(null);
      setAviso("La sesión terminó o fue reemplazada por el acceso desde otro dispositivo. Ingrese nuevamente.");
      return;
    }
    const datos = res.ok ? leerCatalogo(await res.json()) : null;
    if (datos) setCatalogo(datos);
    else setAviso("No fue posible cargar el catálogo.");
  }, []);

  useEffect(() => {
    fetch("/admin/api/session", { cache: "no-store" })
      .then((res) => res.json())
      .then((datos) => {
        setAutenticado(datos.autenticado === true);
        if (datos.autenticado === true) void cargar();
      })
      .catch(() => setAviso("No fue posible verificar la sesión."))
      .finally(() => setCargando(false));
  }, [cargar]);

  useEffect(() => {
    if (!autenticado) return;
    const intervalo = window.setInterval(() => { void cargar(); }, 2000);
    return () => window.clearInterval(intervalo);
  }, [autenticado, cargar]);

  const faltan = catalogo !== null && catalogo.candidates.length < 2;
  useEffect(() => {
    if (!autenticado || !faltan) return;
    const advertir = (evento: BeforeUnloadEvent) => {
      evento.preventDefault();
      evento.returnValue = true;
    };
    const navegar = (evento: MouseEvent) => {
      const enlace = (evento.target as Element).closest("a[href]");
      if (enlace && enlace instanceof HTMLAnchorElement && !enlace.href.startsWith(`${location.origin}/admin`) &&
          !window.confirm("Solo hay un candidato. Cree otro antes de salir para habilitar la votación. ¿Salir de todas formas?")) {
        evento.preventDefault();
        evento.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", advertir);
    document.addEventListener("click", navegar, true);
    return () => {
      window.removeEventListener("beforeunload", advertir);
      document.removeEventListener("click", navegar, true);
    };
  }, [autenticado, faltan]);

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
      void cargar();
    } catch { setAviso("No fue posible iniciar sesión."); }
    finally { setOcupado(false); }
  }

  async function cerrar() {
    if (faltan && !window.confirm("Solo hay un candidato. Cree otro antes de salir para habilitar la votación. ¿Cerrar sesión de todas formas?")) return;
    try {
      await fetch("/admin/api/session", { method: "DELETE", cache: "no-store" });
    } catch {
      // El estado local también se cierra si el servidor no responde.
    }
    setAutenticado(false);
    setCatalogo(null);
    setUsuario("");
    setPassword("");
    router.refresh();
  }

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

  if (cargando) return <p className="mt-10 text-tinta-suave">Verificando acceso…</p>;
  if (!autenticado) return (
    <form onSubmit={iniciar} className="mt-10 max-w-md border border-tinta p-6 sm:p-8">
      <h2 className="font-serif text-2xl">Acceso administrativo</h2>
      <p className="mt-3 text-sm text-sello">
        Acceda solo por http://localhost:3001/admin con el túnel SSM abierto.
        Network en su propio navegador mostrará la contraseña enviada; fuera del túnel HTTP no la cifra.
      </p>
      <label className="mt-6 block text-sm font-medium">Usuario
        <input className={`${campo} mt-2`} autoComplete="username" value={usuario} onChange={(e) => setUsuario(e.target.value)} required />
      </label>
      <label className="mt-5 block text-sm font-medium">Contraseña
        <input className={`${campo} mt-2`} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      </label>
      <button className={`${boton} mt-6`} disabled={ocupado}>Ingresar</button>
      {aviso && <p role="alert" className="mt-4 text-sello">{aviso}</p>}
    </form>
  );

  return <>
  <div className="mt-10 grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(18rem,22rem)]">
    <section aria-label="Listado de candidatos">
      <div className="flex items-center justify-between border-b-2 border-tinta pb-3">
        <h2 className="font-serif text-2xl">Candidatos ({catalogo?.candidates.length ?? "…" })</h2>
        <button onClick={cerrar} className="text-sm underline underline-offset-4">Cerrar sesión</button>
      </div>
      {faltan && <p role="alert" className="mt-5 border-l-4 border-sello bg-papel-hondo p-4 font-medium">
        Solo hay un candidato. Cree al menos otro antes de salir para habilitar la votación.
      </p>}
      <ul className="divide-y divide-tinta/20">
        {catalogo?.candidates.map((c) => <li key={c.candidateId} className="flex flex-wrap items-center justify-between gap-3 py-5">
          <div><p className="font-serif text-xl">{c.name}</p><p className="text-sm text-tinta-suave">{c.list}</p></div>
          <div className="flex gap-3 text-sm">
            <button onClick={() => editar(c)} className="border border-tinta px-3 py-1 hover:bg-papel-hondo">Editar</button>
            <button disabled={ocupado || catalogo.candidates.length <= 2}
              title={catalogo.candidates.length <= 2 ? "Deben permanecer al menos dos candidatos" : undefined}
              onClick={() => { if (window.confirm(`¿Eliminar a ${c.name}?`)) void mutar("DELETE", c.candidateId); }}
              className="border border-sello px-3 py-1 text-sello disabled:opacity-40">Eliminar</button>
          </div>
        </li>)}
      </ul>
    </section>
    <form onSubmit={(e) => { e.preventDefault(); void mutar(editando ? "PUT" : "POST", editando ?? undefined); }}
      className="h-fit border border-tinta p-6">
      <h2 className="font-serif text-2xl">{editando ? "Editar candidato" : "Agregar candidato"}</h2>
      <label className="mt-5 block text-sm font-medium">Nombre
        <input className={`${campo} mt-2`} maxLength={120} value={nombre} onChange={(e) => setNombre(e.target.value)} required />
      </label>
      <label className="mt-5 block text-sm font-medium">Lista o partido
        <input className={`${campo} mt-2`} maxLength={120} value={lista} onChange={(e) => setLista(e.target.value)} required />
      </label>
      <button className={`${boton} mt-6`} disabled={ocupado}>{editando ? "Guardar cambios" : "Crear candidato"}</button>
      {editando && <button type="button" onClick={() => { setEditando(null); setNombre(""); setLista(""); }}
        className="ml-3 text-sm underline">Cancelar</button>}
      {aviso && <p role="status" className="mt-4 text-sello">{aviso}</p>}
    </form>
  </div>
  <AdministracionPadron regiones={catalogo?.regions ?? []} />
  <section className="mt-8 border border-sello p-6" aria-label="Nueva votación">
    <h2 className="font-serif text-2xl">Iniciar otra votación</h2>
    <p className="mt-2">Conserva los candidatos y el padrón, reinicia los resultados y habilita un nuevo voto por persona.</p>
    <button type="button" disabled={ocupado || !catalogo || catalogo.candidates.length < 2}
      onClick={() => void reiniciarVotacion()}
      className="mt-4 border-2 border-sello px-5 py-2 font-medium text-sello disabled:opacity-50">
      Reiniciar votación
    </button>
  </section>
  </>;
}
