import { config } from "@/lib/config";
import { CODIGO_DEMO } from "@/lib/demo";

export function AvisoDemo() {
  if (!config.demo) {
    return null;
  }
  return (
    <div role="status" className="sticky top-0 z-20 border-b-2 border-sello bg-papel-hondo">
      <p className="mx-auto max-w-5xl px-4 py-2 text-sm">
        <strong className="font-semibold uppercase tracking-wider text-sello">Datos simulados.</strong>{" "}
        Esta versión funciona sin conexión con la plataforma. Para probar el tarjetón use el código{" "}
        <span className="font-mono break-all">{CODIGO_DEMO}</span>.
      </p>
    </div>
  );
}
