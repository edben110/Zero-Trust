"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const ENLACES = [
  { href: "/mi-codigo", texto: "Mi código de votante" },
  { href: "/", texto: "Tarjetón" },
  { href: "/resultados", texto: "Escrutinio nacional" },
  { href: "/resultados/regiones", texto: "Por región" },
] as const;

export function Navegacion() {
  const ruta = usePathname();
  return (
    <nav aria-label="Secciones">
      <ul className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
        {ENLACES.map((enlace) => {
          const actual = ruta === enlace.href;
          return (
            <li key={enlace.href}>
              <Link
                href={enlace.href}
                aria-current={actual ? "page" : undefined}
                className={
                  actual
                    ? "border-b-2 border-tinta pb-0.5 font-semibold"
                    : "border-b border-transparent pb-0.5 text-tinta-suave hover:border-tinta hover:text-tinta"
                }
              >
                {enlace.texto}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
