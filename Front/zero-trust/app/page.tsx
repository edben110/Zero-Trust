import type { Metadata } from "next";
import { Tarjeton } from "@/components/Tarjeton";

export const metadata: Metadata = { title: "Tarjetón" };

export default function PaginaTarjeton() {
  return (
    <div className="grid gap-8">
      <header className="max-w-2xl">
        <h1 className="font-serif text-4xl sm:text-5xl">Tarjetón electoral</h1>
        <p className="mt-3 text-tinta-suave">
          Escriba su código de votante, seleccione su región y marque un candidato. Antes de registrarse, su voto se
          valida contra el padrón.
        </p>
      </header>
      <Tarjeton />
    </div>
  );
}
