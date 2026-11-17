import type { Metadata } from "next";
import { EscrutinioNacional } from "@/components/EscrutinioNacional";

export const metadata: Metadata = { title: "Escrutinio nacional" };

export default function PaginaEscrutinioNacional() {
  return (
    <div className="grid gap-6">
      <h1 className="sr-only">Resultados nacionales en vivo</h1>
      <EscrutinioNacional />
    </div>
  );
}
