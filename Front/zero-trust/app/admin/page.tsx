import type { Metadata } from "next";
import { AdministracionCandidatos } from "@/components/AdministracionCandidatos";

export const metadata: Metadata = { title: "Administrar candidatos" };

export default function AdminPage() {
  return (
    <main id="contenido" className="mx-auto max-w-5xl px-5 py-12 sm:px-8">
      <p className="text-sm font-semibold uppercase tracking-widest text-sello">Mesa electoral · Administración</p>
      <h1 className="mt-3 font-serif text-4xl sm:text-5xl">Candidatos del tarjetón</h1>
      <p className="mt-4 max-w-2xl text-tinta-suave">
        Los cambios se publican en el catálogo de votación. Para habilitar una votación se requieren al menos dos candidatos.
      </p>
      <AdministracionCandidatos />
    </main>
  );
}
