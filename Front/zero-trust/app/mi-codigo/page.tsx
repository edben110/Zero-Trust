import type { Metadata } from "next";
import { ConsultaCodigo } from "@/components/ConsultaCodigo";

export const metadata: Metadata = { title: "Mi código de votante" };

export default function PaginaMiCodigo() {
  return (
    <div className="grid gap-8">
      <header className="max-w-2xl">
        <h1 className="font-serif text-4xl sm:text-5xl">Mi código de votante</h1>
        <p className="mt-3 text-tinta-suave">
          Escriba su número de cédula para obtener el código con el que votará en esta elección. La cédula solo se
          usa para calcular el código y no se guarda.
        </p>
      </header>
      <ConsultaCodigo />
    </div>
  );
}
