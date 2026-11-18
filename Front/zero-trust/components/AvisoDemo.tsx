import { config } from "@/lib/config";
import { CEDULA_DEMO, CODIGO_DEMO } from "@/lib/demo";

export function AvisoDemo() {
  if (!config.demo) {
    return null;
  }
  return (
    <div role="status" className="z-20 border-b-2 sm:sticky sm:top-0 border-sello bg-papel">
      <p className="mx-auto max-w-5xl px-4 py-2 text-sm">
        <strong className="font-semibold uppercase tracking-wider text-sello">Datos simulados.</strong>{" "}
        Esta versión funciona sin conexión con la plataforma. Para probar el tarjetón use el código{" "}
        <span className="font-mono break-all">{CODIGO_DEMO}</span>, que corresponde a la cédula{" "}
        <span className="font-mono">{CEDULA_DEMO}</span>.
      </p>
    </div>
  );
}
