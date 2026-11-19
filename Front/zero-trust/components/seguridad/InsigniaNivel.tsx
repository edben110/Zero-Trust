import type { ReactNode } from "react";
import { nivel } from "@/lib/seguridad";

export function InsigniaNivel({ valor, children }: { valor: string; children?: ReactNode }) {
  return (
    <span className={`border px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${nivel(valor).clase}`}>
      {children ?? nivel(valor).etiqueta}
    </span>
  );
}
