import type { Metadata } from "next";
import { EscrutinioRegional } from "@/components/EscrutinioRegional";

export const metadata: Metadata = { title: "Escrutinio por región" };

export default function PaginaEscrutinioRegional() {
  return (
    <div className="grid gap-6">
      <h1 className="sr-only">Resultados por región en vivo</h1>
      <EscrutinioRegional />
    </div>
  );
}
