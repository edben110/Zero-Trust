# Despliegue con Docker Compose (un compose por VM)

Cada VM tiene su `docker-compose.yml` con solo los contenedores de su zona. Docker es el runtime dentro de la VM; no hay un compose unificado.

```
deploy/
  vm-auth/     auth_server
  vm-brokers/  kafka, rabbitmq, postgres-db (+ kafka-init: topics y ACL)
  vm-api/      voting_api
  vm-init/     voter_registration_service (tarea unica)
  vm-core/     user_validation_service x2, analytics_archiver x2, vote_processor,
               regional_rollup_service, bot_detector_service
  vm-app/      global_dashboard_worker x2, regional_dashboard_worker x2, front
```

## Como se obtiene un contenedor
`Dockerfile` -> `docker build` -> imagen -> `docker run` / `docker compose up` -> contenedor.
Compose hace los dos pasos: `docker compose up -d --build` compila las imagenes (`build:`) y arranca los contenedores.

## Pasos en cada VM
1. Clonar el repositorio (el compose usa `../../Back/services/...`, `../../Front/...` y `../../docs/schema.sql`).
2. `cd deploy/<vm> && cp .env.example .env` y completar los valores.
3. Crear los archivos de la tabla siguiente.
4. `docker compose up -d --build`.

Orden: vm-auth, vm-brokers, vm-init, vm-core, vm-api, vm-app. Los servicios que se conectan a Kafka tienen `restart` y reintentan hasta que auth_server y los brokers respondan.

## Archivos que deben existir (no se suben al repositorio)
| VM | Ruta en `deploy/<vm>/` | Contenido |
|---|---|---|
| vm-auth | `secrets/firma.pem` | Clave privada RSA de firma |
| vm-auth | `secrets/tls.p12` | Keystore PKCS12 de TLS (CN/SAN `vm-auth`) |
| vm-auth | `secrets/clientes.json` | Clientes y hashes BCrypt (formato en `Back/services/auth_server/config/README.md`) |
| vm-brokers | `secrets/kafka.keystore.p12` | Keystore PKCS12 del listener de Kafka (CN/SAN `vm-brokers`) |
| vm-brokers | `secrets/truststore.p12` | Truststore con el certificado de auth_server |
| vm-brokers | `secrets/ca.pem` | Certificado/CA de auth_server en PEM (RabbitMQ lo usa para leer el JWKS) |
| vm-api, vm-init, vm-core, vm-app | `secrets/truststore.p12` | Truststore con los certificados de auth_server y de Kafka |
| vm-api | `config/catalogo.json` | Basado en `Back/services/voting_api/config/catalogo.ejemplo.json` |
| vm-init | `config/votantes.json` | Basado en `Back/services/voter_registration_service/config/votantes.ejemplo.json` |

Los archivos montados los lee el usuario `appuser` del contenedor (uid 100): deben ser legibles por otros (`chmod 644`) o tener ese propietario.

## Codigo de votante y voto unico
- `vm-api` y `vm-init` deben tener el mismo `ELECTION_ID` y la misma `VOTER_HMAC_KEY` (Base64, minimo 32 bytes; `openssl rand -base64 32`). La clave es secreta y no debe cambiar durante la eleccion.
- `voting_api` usa PostgreSQL (tabla `voter_election_usage`): nftables debe permitir `vm-api -> vm-brokers:5432`.
- El padron (`VOTERS_FILE`) trae cedulas; `voter_registration_service` guarda solo el UUID derivado.
- La cedula viaja en el cuerpo de `POST /voter-id`: en produccion hace falta TLS delante de `voting_api` (443).

## Recursos y replicas
Limites de CPU y memoria en `deploy.resources.limits`, y `-XX:MaxRAMPercentage=75.0` en la JVM (definido en cada Dockerfile). Replicas: `analytics_archiver` x2, `user_validation_service` x2 y los dos dashboard workers x2. `vote_processor` y `regional_rollup_service` son una sola instancia (stateful, ver `docs/contratos.md`).

Los dashboard workers publican un puerto por replica: global 8080-8081, regional 8082-8083. El front apunta a la primera replica de cada uno (`PUBLIC_WS_*` en `vm-app/.env`).

## Puntos abiertos
- `bot_detector_service`, `vote_processor`, `regional_rollup_service` y los dos dashboard workers no leen `CLIENT_ID`/`CLIENT_SECRET` en su codigo actual (no tienen cliente de token como los servicios de `config/`). El compose les pasa las variables y el truststore, pero no podran autenticarse en Kafka/RabbitMQ hasta que implementen ese cliente. Ademas apuntan a la clase `...oauthbearer.secured.OAuthBearerLoginCallbackHandler`, obsoleta desde Kafka 3.4 (ver `docs/contratos.md`).
- `voting_api` solo habla HTTP (80). Para 443 hace falta un proxy TLS delante.
- RabbitMQ escucha AMQP sin TLS en 5672, como indica el contrato.
- La configuracion OAuth de Kafka y RabbitMQ en `vm-brokers` no se ha probado de extremo a extremo: requiere los certificados y un `auth_server` real. Se valido la sintaxis de los compose y que arrancan Postgres con `docs/schema.sql`.
- El front corre con `next start` y los `node_modules` completos, porque `next.config.ts` necesita `typescript` en ejecucion.

## Prueba local (`deploy/local/`)
Un solo `docker-compose.yml` que simula las 6 VMs en tu maquina, sin crear VMs ni editar `.env`.

```
cd deploy/local
docker compose up -d --build    # la primera vez compila las imagenes y genera los secretos
docker compose down             # detiene (conserva datos y secretos)
docker compose down -v          # detiene y borra todo, incluidos los secretos generados
```

- `secrets-init` genera una CA de desarrollo, los certificados, la clave de firma y `clientes.json` en un volumen. Los secretos de los clientes son valores de desarrollo fijos (se pueden cambiar con variables `SECRET_<CLIENT_ID>`).
- Cada zona es un grupo de contenedores. Las redes `net-auth`, `net-brokers`, `net-db` y `net-public` aproximan la segmentacion; las reglas nftables no se simulan.
- Puertos del host: front 3001 (`FRONT_PORT`), voting_api 8080, auth_server 8443, dashboards 8090-8093.
- Diferencia con el despliegue real: auth_server escucha en 8443, no en 443.

Verificado con esta prueba: secretos y JWT, Kafka y RabbitMQ validando el token, `voter_registration_service` cargando el padron, `POST /vote` (200 con voto valido, 403 si no esta habilitado, 400 si el payload es invalido) y el voto guardado en `vote_audit`. La primera peticion tras arrancar puede dar 503 (`validation_unavailable`): `voting_api` pide token a Kafka y supera el limite de 5 s. Se repite y funciona.

Pendiente (codigo de otros servicios, no de este compose): `vote_processor`, `regional_rollup_service`, `bot_detector_service` y los dos dashboard workers se reinician en bucle porque no tienen cliente de token. Los dos primeros y `bot_detector_service` fallan por falta de JAAS de Kafka; los workers, porque RabbitMQ rechaza su login PLAIN sin JWT.
Tambien hay que acordar los scopes de RabbitMQ: el ejemplo no da `configure` sobre `user_validation_queue`, y la prueba local lo anade en `secrets-init.sh`. Los clientes RabbitMQ usan el vhost `votacion` (`SPRING_RABBITMQ_VIRTUAL_HOST`).
