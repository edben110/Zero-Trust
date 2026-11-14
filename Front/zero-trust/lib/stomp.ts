import { Client } from "@stomp/stompjs";
import type { EstadoConexion } from "./contrato";

// Sin conexión estable tras estos intentos, se muestra "Sin conexión" aunque se siga reintentando.
const INTENTOS_ANTES_DE_SIN_CONEXION = 3;

type Suscripcion = {
  url: string;
  destinos: string[];
  alRecibir: (destino: string, cuerpo: string) => void;
  alCambiarEstado: (estado: EstadoConexion) => void;
};

// Conecta por WebSocket nativo al endpoint STOMP de un dashboard worker (/ws/results/websocket) y reconecta solo.
export function suscribir({ url, destinos, alRecibir, alCambiarEstado }: Suscripcion): () => void {
  if (url === "") {
    alCambiarEstado("sin-conexion");
    return () => {};
  }
  let fallos = 0;
  const cliente = new Client({
    brokerURL: url,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      fallos = 0;
      alCambiarEstado("en-vivo");
      for (const destino of destinos) {
        cliente.subscribe(destino, (mensaje) => alRecibir(destino, mensaje.body));
      }
    },
    onWebSocketClose: () => {
      fallos += 1;
      alCambiarEstado(fallos >= INTENTOS_ANTES_DE_SIN_CONEXION ? "sin-conexion" : "reconectando");
    },
    onStompError: () => alCambiarEstado("reconectando"),
  });
  cliente.activate();
  return () => {
    void cliente.deactivate();
  };
}
