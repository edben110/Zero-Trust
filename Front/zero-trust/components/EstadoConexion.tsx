import type { EstadoConexion as Estado } from "@/lib/contrato";

const TEXTOS: Record<Estado, string> = {
  conectando: "Conectando…",
  "en-vivo": "En vivo",
  reconectando: "Reconectando…",
  "sin-conexion": "Sin conexión",
};

const COLORES: Record<Estado, string> = {
  conectando: "bg-tinta-suave",
  "en-vivo": "bg-verificado",
  reconectando: "bg-tinta-suave",
  "sin-conexion": "bg-sello",
};

export function EstadoConexion({ estado }: { estado: Estado }) {
  return (
    <span role="status" className="inline-flex items-center gap-2 text-sm">
      <span className={`inline-block h-2 w-2 rounded-full ${COLORES[estado]}`} aria-hidden="true" />
      <span className={estado === "sin-conexion" ? "font-semibold text-sello" : undefined}>{TEXTOS[estado]}</span>
    </span>
  );
}
