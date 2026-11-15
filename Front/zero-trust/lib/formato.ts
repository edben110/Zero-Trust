const ZONA = "America/Bogota";

const numero = new Intl.NumberFormat("es-CO");
const porcentaje = new Intl.NumberFormat("es-CO", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const fecha = new Intl.DateTimeFormat("es-CO", { dateStyle: "long", timeZone: ZONA });
const hora = new Intl.DateTimeFormat("es-CO", {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
  timeZone: ZONA,
});

export function formatearNumero(valor: number): string {
  return numero.format(valor);
}

export function formatearPorcentaje(parte: number, total: number): string {
  return total === 0 ? "0,0 %" : `${porcentaje.format((parte / total) * 100)} %`;
}

export function formatearFecha(iso: string): string {
  return fecha.format(new Date(iso));
}

export function formatearHora(iso: string): string {
  return hora.format(new Date(iso));
}

export function nombreRegion(region: string): string {
  return region.charAt(0).toLocaleUpperCase("es-CO") + region.slice(1);
}
