"use client";

import { useEffect } from "react";

// Con un solo candidato la votación no está habilitada: se advierte antes de salir del panel.
export function useAvisoSalida(activo: boolean) {
  useEffect(() => {
    if (!activo) return;
    const advertir = (evento: BeforeUnloadEvent) => {
      evento.preventDefault();
      evento.returnValue = true;
    };
    const navegar = (evento: MouseEvent) => {
      const enlace = (evento.target as Element).closest("a[href]");
      if (enlace && enlace instanceof HTMLAnchorElement && !enlace.href.startsWith(`${location.origin}/admin`) &&
          !window.confirm("Solo hay un candidato. Cree otro antes de salir para habilitar la votación. ¿Salir de todas formas?")) {
        evento.preventDefault();
        evento.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", advertir);
    document.addEventListener("click", navegar, true);
    return () => {
      window.removeEventListener("beforeunload", advertir);
      document.removeEventListener("click", navegar, true);
    };
  }, [activo]);
}
