// El panel va por HTTP (túnel SSM) y navigator.clipboard exige contexto seguro: si falla, se usa la selección temporal.
export async function copiarTexto(texto: string): Promise<boolean> {
  if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(texto);
      return true;
    } catch {
      // Sin permiso o sin contexto seguro: se intenta el respaldo.
    }
  }
  return copiarConSeleccion(texto);
}

function copiarConSeleccion(texto: string): boolean {
  if (typeof document === "undefined" || typeof document.execCommand !== "function") {
    return false;
  }
  const activo = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  const area = document.createElement("textarea");
  area.value = texto;
  area.setAttribute("readonly", "");
  area.setAttribute("aria-hidden", "true");
  // Fuera de la vista pero presente en el DOM: execCommand necesita un campo real.
  area.style.position = "fixed";
  area.style.top = "0";
  area.style.left = "0";
  area.style.width = "1px";
  area.style.height = "1px";
  area.style.padding = "0";
  area.style.border = "none";
  area.style.opacity = "0";
  document.body.appendChild(area);
  let copiado = false;
  try {
    area.focus();
    area.select();
    area.setSelectionRange(0, texto.length);
    copiado = document.execCommand("copy");
  } catch {
    copiado = false;
  } finally {
    area.remove();
    activo?.focus();
  }
  return copiado;
}
