"use client";

import { useSeguridad } from "@/lib/useSeguridad";
import { FormularioPrueba } from "./seguridad/FormularioPrueba";
import { MantenimientoSeguridad } from "./seguridad/MantenimientoSeguridad";
import { ResumenSeguridad } from "./seguridad/ResumenSeguridad";
import { TablaIncidentes } from "./seguridad/TablaIncidentes";
import { TablaIps } from "./seguridad/TablaIps";

export function AdministracionSeguridad() {
  const { datos, aviso, setAviso, ocupado, actualizadoEn, llamar, actualizar, purgar } = useSeguridad();

  if (!datos) return (
    <section aria-label="Detección de ataques" className="mt-12 border-t-2 border-tinta pt-8">
      <h2 className="font-serif text-2xl">Detección de ataques por IP</h2>
      <p className="mt-2 text-sm text-tinta-suave">Consultando el estado del detector…</p>
      <p className="mt-1 text-xs text-tinta-suave">
        Si esta sección no carga, la sesión pudo expirar: vuelva a iniciar sesión o pulse «Reintentar».
      </p>
      <button type="button" onClick={() => void actualizar()} disabled={ocupado}
        className="mt-4 border-2 border-tinta px-5 py-2 font-medium text-tinta disabled:opacity-50">
        {ocupado ? "Consultando…" : "Reintentar"}
      </button>
    </section>
  );

  return (
    <section aria-labelledby="titulo-seguridad" className="mt-12 border-t-2 border-tinta pt-8">
      <h2 id="titulo-seguridad" className="font-serif text-2xl">Detección de ataques por IP</h2>
      <ResumenSeguridad thresholds={datos.thresholds} summary={datos.summary} />
      <MantenimientoSeguridad ocupado={ocupado} actualizadoEn={actualizadoEn}
        onActualizar={() => void actualizar()} onPurgar={() => void purgar()} />
      <FormularioPrueba ocupado={ocupado} aviso={aviso} onAviso={setAviso} llamar={llamar} />
      <TablaIps ips={datos.ips} />
      <TablaIncidentes incidents={datos.incidents} />
    </section>
  );
}
