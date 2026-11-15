import type { Metadata } from "next";
import { Fraunces, IBM_Plex_Mono, IBM_Plex_Sans } from "next/font/google";
import Link from "next/link";
import { AvisoDemo } from "@/components/AvisoDemo";
import { Navegacion } from "@/components/Navegacion";
import "./globals.css";

const serif = Fraunces({ subsets: ["latin"], variable: "--fuente-serif", display: "swap" });
const sans = IBM_Plex_Sans({ subsets: ["latin"], weight: ["400", "500", "600"], variable: "--fuente-sans", display: "swap" });
const mono = IBM_Plex_Mono({ subsets: ["latin"], weight: ["400", "500"], variable: "--fuente-mono", display: "swap" });

export const metadata: Metadata = {
  title: { default: "Mesa de votación", template: "%s · Mesa de votación" },
  description: "Tarjetón electoral y escrutinio en vivo de la plataforma de votación Zero Trust.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es-CO" className={`${serif.variable} ${sans.variable} ${mono.variable}`}>
      <body className="flex min-h-screen flex-col bg-papel text-tinta">
        <a
          href="#contenido"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-30 focus:bg-papel focus:px-3 focus:py-2"
        >
          Saltar al contenido
        </a>
        <AvisoDemo />
        <header className="border-b border-linea">
          <div className="mx-auto flex max-w-5xl flex-wrap items-end justify-between gap-4 px-4 py-5">
            <Link href="/" className="block">
              <span className="block font-serif text-2xl leading-none">Mesa de votación</span>
              <span className="mt-1 block text-xs uppercase tracking-[0.2em] text-tinta-suave">
                Jornada electoral de prueba
              </span>
            </Link>
            <Navegacion />
          </div>
        </header>
        <main id="contenido" className="mx-auto w-full max-w-5xl flex-1 px-4 py-10">
          {children}
        </main>
        <footer className="border-t border-linea">
          <p className="mx-auto max-w-5xl px-4 py-5 text-xs text-tinta-suave">
            Proyecto académico de votación distribuida. Cada voto se valida contra el padrón antes de contarse.
          </p>
        </footer>
      </body>
    </html>
  );
}
