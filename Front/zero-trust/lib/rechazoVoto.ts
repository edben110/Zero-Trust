import type { ResultadoVoto } from "./contrato";

const MENSAJES_CAMPO: Record<string, string> = {
  voterId: "El código de votante no es válido.",
  candidateId: "El candidato marcado no está en el tarjetón.",
  region: "La región seleccionada no está en el tarjetón.",
};

// Texto que ve el votante para cada respuesta de POST /vote que no es un voto aceptado.
export function mensajeDeRechazo(resultado: Exclude<ResultadoVoto, { tipo: "aceptado" }>): string {
  switch (resultado.tipo) {
    case "invalido":
      return resultado.campos.length === 0
        ? "El voto no se pudo leer. Revise los datos e intente de nuevo."
        : resultado.campos.map((campo) => MENSAJES_CAMPO[campo] ?? `Revise el campo ${campo}.`).join(" ");
    case "no-habilitado":
      return "Este código no está habilitado para votar.";
    case "ya-voto":
      return "Ya se registró un voto para este documento en esta elección.";
    case "region-incorrecta":
      return "error esa no es la region que ud designo. Le queda un intento para seleccionar la región registrada.";
    case "anulado":
      return "Su voto quedó anulado por dos selecciones incorrectas de región. No puede votar en esta votación.";
    case "no-disponible":
      return "La validación del votante no está disponible en este momento. Su voto no se registró; intente de nuevo en unos minutos.";
    case "error":
      return "Ocurrió un error inesperado. Su voto no se registró.";
  }
}
