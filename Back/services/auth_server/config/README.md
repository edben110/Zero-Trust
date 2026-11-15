# Archivo de clientes de auth_server

`clientes.ejemplo.json` muestra el formato que lee auth_server desde `AUTH_CLIENTS_FILE`. No sirve para desplegar: cada `secret_hash` se calculó a partir de un secreto aleatorio que se descartó, así que nadie puede obtener un token con él. Los scopes de RabbitMQ (`vhost` "votacion") son ficticios hasta que el equipo acuerde el catálogo en `docs/contratos.md`.

Para el archivo real (nunca se sube al repositorio; `config/clientes.json` está en `.gitignore`):

- Un cliente por servicio, con `client_id` igual al nombre del servicio.
- `secret_hash`: BCrypt de coste 12, el mismo para todos los clientes.
- Secretos generados al azar, solo con los caracteres `A-Z a-z 0-9 . _ ~ -` (Kafka 3.6 los envía en Basic sin codificar).
- `scopes: []` solo para los clientes que únicamente usan Kafka (`audiences: ["kafka"]`).
