"use client";

import { useState } from "react";
import { useAvisoSalida } from "@/lib/useAvisoSalida";
import { useCatalogoAdmin } from "@/lib/useCatalogoAdmin";
import { useSesionAdmin } from "@/lib/useSesionAdmin";
import { AdministracionPadron } from "./AdministracionPadron";
import { AdministracionSeguridad } from "./AdministracionSeguridad";
import { AccesoAdmin } from "./candidatos/AccesoAdmin";
import { FormularioCandidato } from "./candidatos/FormularioCandidato";
import { ListaCandidatos } from "./candidatos/ListaCandidatos";
import { NuevaVotacion } from "./candidatos/NuevaVotacion";

export function AdministracionCandidatos() {
  const [aviso, setAviso] = useState("");
  const sesion = useSesionAdmin(setAviso);
  const admin = useCatalogoAdmin(sesion.autenticado, sesion.perder, setAviso);
  useAvisoSalida(sesion.autenticado && admin.faltan);

  async function cerrar() {
    if (admin.faltan && !window.confirm("Solo hay un candidato. Cree otro antes de salir para habilitar la votación. ¿Cerrar sesión de todas formas?")) return;
    await sesion.cerrar(admin.limpiar);
  }

  if (sesion.cargando) return <p className="mt-10 text-tinta-suave">Verificando acceso…</p>;
  if (!sesion.autenticado) return (
    <AccesoAdmin usuario={sesion.usuario} password={sesion.password} ocupado={sesion.ocupado} aviso={aviso}
      setUsuario={sesion.setUsuario} setPassword={sesion.setPassword} onIniciar={sesion.iniciar} />
  );

  const { catalogo } = admin;
  return <>
  <div className="mt-10 grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(18rem,22rem)]">
    <ListaCandidatos catalogo={catalogo} faltan={admin.faltan} ocupado={admin.ocupado}
      onCerrar={() => void cerrar()} onEditar={admin.editar}
      onEliminar={(c) => { if (window.confirm(`¿Eliminar a ${c.name}?`)) void admin.mutar("DELETE", c.candidateId); }} />
    <FormularioCandidato editando={admin.editando} nombre={admin.nombre} lista={admin.lista}
      ocupado={admin.ocupado} aviso={aviso} setNombre={admin.setNombre} setLista={admin.setLista}
      onGuardar={() => void admin.mutar(admin.editando ? "PUT" : "POST", admin.editando ?? undefined)}
      onCancelar={admin.cancelar} />
  </div>
  <AdministracionPadron regiones={catalogo?.regions ?? []} />
  <AdministracionSeguridad />
  <NuevaVotacion deshabilitado={admin.ocupado || !catalogo || catalogo.candidates.length < 2}
    onReiniciar={() => void admin.reiniciarVotacion()} />
  </>;
}
