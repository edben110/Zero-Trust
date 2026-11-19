# Contratos entre servicios

> Pendiente de acordar con todo el equipo. Nada se implementa contra un valor que no esté aquí.

> **Propuesto por Sebas, pendiente de validar con el equipo:** fila y jar de auth_server, convención de finalName, "Variables de entorno de auth_server", aud por cliente en "Token JWT", "RabbitMQ OAuth2", "Tráfico hacia vm-auth:443", el refresco de token de los clientes Kafka, las filas de voting_api, user_validation_service, voter_registration_service y analytics_archiver, "Cliente de token", "Catálogo", "Respuestas de POST /vote", "Variables de entorno de los servicios de Sebas y del front" y "Tráfico adicional".

## Decisiones
- Estados del bot_detector: **NORMAL / SOSPECHOSA / BLOQUEADA** (propuesto Daniel: se usa NORMAL como estado inicial, alineado con el enunciado)
- Un voto por votante y elección (propuesto, ver "Código de votante y voto único"): voting_api lo garantiza con UNIQUE(election_id, voter_uuid) en `voter_election_usage`.
- Réplicas: analytics_archiver ×2, user_validation_service ×2, global_dashboard_worker ×2, regional_dashboard_worker ×2. vote_processor y regional_rollup_service instancia única (stateful)

## Servicios
| Servicio | VM | Puerto | Jar (finalName) | client_id |
|---|---|---|---|---|
| auth_server | vm-auth | 443 en la VM → 8443 en el contenedor (SERVER_PORT) | auth-server.jar | — (es el emisor) |
| voting_api | vm-api | 8080 en el contenedor | voting-api.jar | voting_api |
| user_validation_service | vm-core | sin puerto HTTP | user-validation-service.jar | user_validation_service |
| voter_registration_service | vm-init | sin puerto (tarea única) | voter-registration-service.jar | voter_registration_service |
| analytics_archiver | vm-core | sin puerto HTTP | analytics-archiver.jar | analytics_archiver |

finalName = nombre del servicio con guiones (auth-server, voting-api, user-validation-service…); el jar queda en target/<finalName>.jar

## Variables de entorno comunes
AUTH_TOKEN_URL, AUTH_JWKS_URL, CLIENT_ID, CLIENT_SECRET, KAFKA_BOOTSTRAP, RABBIT_HOST, RABBIT_PORT, DB_URL, DB_USER, DB_PASSWORD

## Variables de entorno de auth_server
| Variable | Obligatoria | Descripción |
|---|---|---|
| AUTH_ISSUER | sí | Valor del claim iss |
| AUTH_CLIENTS_FILE | sí | Ruta del archivo JSON de clientes (client_id, secret_hash BCrypt, scopes, audiences) |
| AUTH_SIGNING_KEY_FILE | sí | Ruta de la clave privada RSA (PEM) de firma |
| AUTH_SIGNING_KEY_ID | sí | kid de la clave de firma |
| AUTH_TOKEN_TTL | no (300) | Vida del token en segundos |
| AUTH_TLS_KEYSTORE | sí | Ruta del keystore PKCS12 de TLS |
| AUTH_TLS_KEYSTORE_PASSWORD | sí | Contraseña del keystore TLS |
| SERVER_PORT | no (8443) | Puerto HTTPS dentro del contenedor |

## Token JWT (auth_server)
- POST /token, OAuth2 client_credentials (form-urlencoded; credenciales por Basic o en el body)
- Respuesta: {access_token, token_type: "Bearer", expires_in}
- RS256, JWKS en /.well-known/jwks.json
- Claims: iss, sub (= client_id), aud, scope, iat, exp
- aud: por cliente según el archivo de clientes (kafka y/o rabbitmq)

## RabbitMQ OAuth2
- resource_server_id = rabbitmq (coincide con el aud "rabbitmq")
- Formato de scope: rabbitmq.<configure|write|read>:<vhost>/<recurso>
- Kafka no usa scopes: autoriza con ACL sobre el principal sub = client_id (propuesto)

## Tráfico hacia vm-auth:443 (para nftables)
| Origen | Para qué |
|---|---|
| vm-init, vm-api, vm-core, vm-app | Pedir tokens (POST /token) |
| vm-brokers | Leer el JWKS (Kafka y RabbitMQ) |

## Kafka
| Topic | Productor | Consumidor | Key | Payload (JSON) |
|---|---|---|---|---|
| eligible_voters (compactado) | voter_registration_service, voting_api (altas del panel de administración) | user_validation_service | voterId | `{"voterId":"<uuid>","region":"<string>","registeredAt":"<ISO-8601>"}` |
| raw_votes | voting_api | vote_processor, regional_rollup_service, bot_detector_service, analytics_archiver | voteId | `{"voteId":"<uuid>","voterId":"<uuid>","candidateId":"<uuid>","region":"<string>","sourceIp":"<string>","timestamp":"<ISO-8601>"}` |
| security_alerts | bot_detector_service | — | ip | `{"ip":"<string>","riskLevel":"NORMAL\|SOSPECHOSA\|BLOQUEADA","voteCount":<int>,"windowSeconds":<int>,"origin":"LIVE\|DRILL","kind":"IP_PATTERN\|DUPLICATE_VOTE\|WRONG_REGION","detectedAt":"<ISO-8601>"}` |
| security_drills | voting_api (panel de administración) | bot_detector_service | ip | `{"action":"EVALUATE\|RESET\|RESET_ALL","ip":"<string>","votes":<int>}` |
| fraud_events | voting_api | bot_detector_service | ip | `{"kind":"DUPLICATE_VOTE\|WRONG_REGION","ip":"<string>"}` |

- `security_drills` permite probar la detección con IPs reales sin escribir en `raw_votes`: el detector aplica el mismo patrón State y marca el incidente con `origin=DRILL`, distinguible de `LIVE`. `RESET_ALL` es la purga administrativa: el detector olvida todos sus contextos en memoria.
- `fraud_events` transporta incidentes electorales que no son patrones de tráfico: voto duplicado y voto anulado por región incorrecta. bot_detector_service los persiste en `fraud_incidents` con `origin=LIVE` y el `kind` correspondiente, y los republica en `security_alerts`.
- Con TTL de 300 s, los clientes Kafka deben usar `sasl.login.refresh.buffer.seconds=30` (propuesto Sebas).

## RabbitMQ
| Nombre | Tipo | Productor | Consumidor | Payload |
|---|---|---|---|---|
| user_validation_queue | cola RPC (reply-to + correlationId) | voting_api | user_validation_service | request: `{"voterId":"<uuid>"}` / response: `{"valid":true\|false}` |
| live_results_global | fanout exchange | vote_processor | global_dashboard_worker | `{"timestamp":"<ISO-8601>","results":[{"candidateId":"<uuid>","candidateName":"<string>","voteCount":<long>}]}` |
| live_results_regional | topic exchange, routing key = `results.<region>` | regional_rollup_service | regional_dashboard_worker | `{"region":"<string>","timestamp":"<ISO-8601>","results":[{"candidateId":"<uuid>","candidateName":"<string>","voteCount":<long>}]}` |

## API pública
POST /vote payload:
```json
{
  "voterId": "<uuid>",
  "candidateId": "<uuid>",
  "region": "<string>"
}
```
La IP la extrae voting_api del request (X-Forwarded-For o RemoteAddr).

POST /voter-id (propuesto): consulta del código de votante a partir de la cédula.
```json
{ "cedula": "<string>" }
```
| Código | Cuerpo | Cuándo |
|---|---|---|
| 200 | `{"voterId":"<uuid>"}` (con `Cache-Control: no-store`) | El titular está en el padrón (consulta RPC a user_validation_service) |
| 404 | `{"error":"voter_not_registered","message":"El titular de este documento no se encuentra habilitado en el padrón. Por favor pídale al administrador que lo añada."}` | El titular no está en el padrón. No se entrega el código |
| 503 | `{"error":"validation_unavailable"}` | No se pudo consultar el padrón |
| 400 | `{"error":"invalid_request","fields":["cedula"]}` | Cédula ausente, numérica o con formato inválido |

POST /internal/admin/voters (propuesto): alta en el padrón desde el panel de administración. Solo la llama el servidor del front con la cabecera `X-Admin-Token` (`ADMIN_API_TOKEN`).
```json
{ "cedula": "<string>", "region": "<string>" }
```
El nombre del votante no se solicita ni se almacena: `registered_voters` conserva solo el UUID derivado, la región y la fecha de inscripción.
| Código | Cuerpo | Cuándo |
|---|---|---|
| 201 | `{"registered":true}` | Inscrito en `registered_voters` y publicado en `eligible_voters` (misma transacción) |
| 400 | `{"error":"invalid_request","fields":[...]}` | Cédula o región inválidas; la región debe estar en el catálogo vigente |
| 401 | — | Sin token o token incorrecto |
| 409 | `{"error":"already_registered","message":"..."}` | Ese documento ya está en el padrón de la elección |
| 503 | `{"error":"validation_unavailable"}` | Kafka o PostgreSQL no confirmaron. No se inscribió nada |

### Seguridad (solo BFF autenticado, cabecera `X-Admin-Token`)
| Ruta | Efecto |
|---|---|
| `GET /internal/admin/security/incidents` | Estado vigente por IP (`ip_risk_state`), umbrales e histórico de `fraud_incidents` con `kind` y `origin` |
| `POST /internal/admin/security/drills` | Publica N `EVALUATE` en `security_drills`; el incidente queda con `origin=DRILL` |
| `POST /internal/admin/security/reset` | Publica `RESET` para una IP: vuelve a NORMAL sin borrar el histórico |
| `POST /internal/admin/security/purge` | Publica `RESET_ALL` (olvida contextos en memoria) y borra `fraud_incidents` e `ip_risk_state` |

Un voto duplicado y un voto anulado por región incorrecta no se publican en `raw_votes`; voting_api los envía a `fraud_events` y bot_detector_service los guarda como `DUPLICATE_VOTE` y `WRONG_REGION` sin alterar el escrutinio.

## Cliente de token (todos los servicios)
> Propuesto por Sebas, pendiente de validar con el equipo. Comprobado contra kafka-clients 3.6.2 y amqp-client 5.19.0 (los que gestiona Spring Boot 3.2.5).

Variables: `AUTH_TOKEN_URL`, `CLIENT_ID`, `CLIENT_SECRET`, `KAFKA_SECURITY_PROTOCOL` (por defecto `SASL_SSL`), `TRUSTSTORE_FILE` (PKCS12 con la CA del proyecto) y `TRUSTSTORE_PASSWORD`. Si falta una obligatoria, el servicio no arranca.

**Kafka** (solo propiedades; equivalentes en `application.properties`):
```properties
spring.kafka.security.protocol=${KAFKA_SECURITY_PROTOCOL:SASL_SSL}
spring.kafka.properties.sasl.mechanism=OAUTHBEARER
spring.kafka.properties.sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler
spring.kafka.properties.sasl.oauthbearer.token.endpoint.url=${AUTH_TOKEN_URL}
spring.kafka.properties.sasl.login.refresh.buffer.seconds=30
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId="${CLIENT_ID}" clientSecret="${CLIENT_SECRET}" ssl.truststore.location="${TRUSTSTORE_FILE}" ssl.truststore.password="${TRUSTSTORE_PASSWORD}" ssl.truststore.type="PKCS12";
spring.kafka.ssl.trust-store-location=file:${TRUSTSTORE_FILE}
spring.kafka.ssl.trust-store-password=${TRUSTSTORE_PASSWORD}
spring.kafka.ssl.trust-store-type=PKCS12
```
- La clase del handler es `org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler`. La del paquete `...oauthbearer.secured` está obsoleta desde Kafka 3.4.
- Las opciones `ssl.*` dentro del JAAS son las del HTTPS hacia `AUTH_TOKEN_URL`; las de `spring.kafka.ssl.*` son las del TLS hacia el broker.
- Kafka 3.6 envía `clientId:clientSecret` por Basic **sin** codificarlos. Por eso los `client_secret` solo usan los caracteres `A-Z a-z 0-9 . _ ~ -`, que no cambian al decodificar form-urlencoded en auth_server.
- En los servicios de Sebas, el JAAS lo construye y valida una clase de `config/` (con las comillas escapadas), y no una línea de propiedades:
  - `VariablesClienteToken` lee y valida las variables al arrancar; `KAFKA_SECURITY_PROTOCOL` solo admite `SASL_SSL` o `SASL_PLAINTEXT`.
  - `PropiedadesKafkaOAuth` construye las propiedades de arriba.
  - `ConfiguracionKafkaOAuth` las aplica a las fábricas de productores y consumidores (`DefaultKafkaProducerFactoryCustomizer` y `DefaultKafkaConsumerFactoryCustomizer` de Spring Boot 3.2.5).
- Un consumidor de Kafka pide el token a auth_server al crearse, durante el arranque. Si auth_server no responde, el servicio no arranca: en el despliegue conviene una política de reinicio (`restart: on-failure` o equivalente). Un productor pide el token en el primer envío.

**RabbitMQ** (clase de `config/` sobre el `ConnectionFactory` de Spring AMQP):
- `OAuth2ClientCredentialsGrantCredentialsProvider` (amqp-client) con `tokenEndpointUri=${AUTH_TOKEN_URL}`, `clientId=${CLIENT_ID}`, `clientSecret=${CLIENT_SECRET}`, `grantType=client_credentials` y un `SSLContext` creado con el truststore (`TRUSTSTORE_FILE`/`TRUSTSTORE_PASSWORD`).
- `DefaultCredentialsRefreshService` en el `ConnectionFactory`: renueva el token antes de que caduque (al 80 % de su vida por defecto).
- Sin usuario ni contraseña fijos. amqp-client codifica las credenciales en Basic como pide RFC 6749.
- En los servicios de Sebas lo hace `ConfiguracionRabbitOAuth` con un `ConnectionFactoryCustomizer` de Spring Boot 3.2.5, que se aplica después de que Boot configure la conexión.

**Código copiado a propósito**: el cliente de token no es una librería compartida porque cada Dockerfile compila un solo servicio con su propio `pom.xml`; una librería obligaría a cambiar el contexto de build de todos los Dockerfiles. Por eso estas clases de `config/` están copiadas (solo cambia el `package`) en voting_api, user_validation_service, voter_registration_service y analytics_archiver: `ContextoTls`, `PropiedadesKafkaOAuth`, `VariablesClienteToken`, `ConfiguracionClienteToken` y `ConfiguracionKafkaOAuth`; `ConfiguracionRabbitOAuth` está en voting_api y user_validation_service. Un cambio en una copia se aplica a mano en las demás.

**PostgreSQL**: `DB_URL`, `DB_USER`, `DB_PASSWORD`, sin valores por defecto; `spring.jpa.hibernate.ddl-auto=validate` contra `docs/schema.sql`.

## Catálogo
> Propuesto por Sebas, pendiente de validar con el equipo.

`voting_api` expone `GET /catalog`, leído al arrancar del archivo `CATALOG_FILE`:
```json
{"candidates":[{"candidateId":"<uuid>","name":"<string>","list":"<string>"}],"regions":["<string>"]}
```

## Respuestas de POST /vote
> Propuesto por Sebas, pendiente de validar con el equipo.

| Código | Cuerpo | Cuándo |
|---|---|---|
| 200 | `{"voteId":"<uuid>","receivedAt":"<ISO-8601>"}` | Votante habilitado, región coincidente con el padrón y voto publicado en `raw_votes` |
| 400 | `{"error":"invalid_request","fields":[...]}` | Payload inválido, candidato o región fuera del catálogo |
| 403 | `{"error":"voter_not_eligible"}` | user_validation_service respondió `{"valid":false}` |
| 409 | `{"error":"already_voted","message":"Ya se registró un voto para este documento en esta elección"}` | Ese voterId ya votó en esta elección |
| 409 | `{"error":"vote_invalidated","message":"..."}` | Segunda selección de una región distinta de la registrada: el voto queda anulado en la jornada |
| 422 | `{"error":"wrong_region","message":"error esa no es la region que ud designo"}` | Primera selección de una región distinta de la registrada. Queda un único intento |
| 503 | `{"error":"validation_unavailable"}` | Sin respuesta RPC en `RPC_TIMEOUT_MS`, broker o PostgreSQL caídos. No se registró nada: se puede reintentar |

La región enviada se compara en el servidor con `registered_voters.region`; no se confía en la selección del navegador. Los fallos se cuentan por jornada (`voter_region_attempts`): el primero responde 422, el segundo anula el voto con 409 y, desde entonces, cualquier intento responde 409 hasta que el administrador inicie otra votación.

`sourceIp` es la dirección remota de la petición. `X-Forwarded-For` solo se usa si `TRUST_FORWARDED_HEADERS=true` (detrás de un proxy de confianza), para que un cliente no pueda falsificar su IP ante bot_detector_service.

## Variables de entorno de los servicios de Sebas y del front
> Propuesto por Sebas, pendiente de validar con el equipo.

| Variable | Servicio | Descripción |
|---|---|---|
| CATALOG_FILE | voting_api | Ruta del JSON del catálogo |
| CORS_ALLOWED_ORIGINS | voting_api | Orígenes permitidos, separados por comas, sin comodín |
| TRUST_FORWARDED_HEADERS | voting_api | `true` solo detrás de un proxy de confianza (por defecto `false`) |
| RPC_TIMEOUT_MS | voting_api | Espera máxima de la respuesta RPC (por defecto 3000) |
| VOTERS_FILE | voter_registration_service | Ruta del JSON con el padrón inicial. Cada registro trae `cedula` y `region` |
| ELECTION_ID | voting_api, voter_registration_service | Elección en curso (1 a 64 caracteres `A-Z a-z 0-9 . _ -`). Igual en ambos |
| VOTER_HMAC_KEY | voting_api, voter_registration_service | Clave HMAC en Base64, mínimo 32 bytes. Igual en ambos, secreta y estable durante la elección |
| DB_URL, DB_USER, DB_PASSWORD | voting_api | PostgreSQL para `voter_election_usage` |
| NEXT_PUBLIC_VOTING_API_URL | front | URL base de voting_api |
| NEXT_PUBLIC_WS_GLOBAL_URL | front | URL STOMP del global_dashboard_worker |
| NEXT_PUBLIC_WS_REGIONAL_URL | front | URL STOMP del regional_dashboard_worker |

## Tráfico adicional (para nftables)
> Propuesto por Sebas, pendiente de validar con el equipo.

| Origen | Destino | Para qué |
|---|---|---|
| vm-init | vm-brokers:5432 | voter_registration_service guarda el padrón en `registered_voters` (`docs/schema.sql`) |
| vm-api | vm-brokers:5432 | voting_api registra el voto único en `voter_election_usage` |
| navegador | vm-api (voting_api) | `POST /voter-id` para consultar el código de votante |
| navegador | vm-api (voting_api) | Tarjetón: `GET /catalog` y `POST /vote` |
| navegador | vm-app (dashboard workers) | WebSocket/STOMP de resultados en vivo |

## Código de votante y voto único
> Propuesto, pendiente de validar con el equipo.

- **Flujo:** la persona escribe su cédula en `/mi-codigo` del front, `POST /voter-id` le devuelve su `voterId` y con él vota en el tarjetón (`POST /vote`, sin cambios en el payload).
- **Cédula:** se normaliza igual en el servidor y en el front (NFKC, sin espacios ni puntos, de 6 a 10 dígitos ASCII, conservando los ceros iniciales). Solo vive en memoria durante la petición: no se guarda, no va en la URL ni en logs ni en mensajes de error.
- **voterId:** UUID versión 8 (RFC 9562) con los primeros 128 bits de `HMAC-SHA-256(VOTER_HMAC_KEY, "zerotrust-voter-v1" 0x00 ELECTION_ID 0x00 cédula)` y los bits de versión y variante. La misma cédula da el mismo UUID en la misma elección y otro en una elección distinta. voting_api y voter_registration_service lo calculan igual. Vector de prueba: clave = bytes 0..31, elección `elecciones-2026`, cédula `0012345678` → `fae9cb04-ba2b-87ef-882c-f39dd70dbaa1`.
- **`/voter-id` avisa si el titular no está en el padrón (404) y no entrega el código.** Esto revela a quien conozca una cédula si está inscrita. El administrador puede inscribirla desde el panel (`POST /internal/admin/voters`); voting_api guarda solo el UUID en `registered_voters` y lo publica en `eligible_voters` (ACL de escritura para `voting_api`).
- **Voto único:** `voter_election_usage(election_id, voter_uuid)` con `UNIQUE (election_id, voter_uuid)`. voting_api inserta con `ON CONFLICT DO NOTHING` y publica en `raw_votes` en la misma transacción: si la publicación falla, se revierte y se puede reintentar. Con solicitudes simultáneas solo una pasa.
- **El voterId permite votar:** quien conozca la cédula de otra persona puede obtener su código. No hay verificación de titularidad.
- **Cambiar la clave o el ELECTION_ID durante la elección cambia todos los UUID:** el padrón dejaría de coincidir y una cédula podría volver a votar.

## Puntos a confirmar
- El enunciado dice que la respuesta RPC es "valido"/"invalido"; el contrato vigente es `{"valid":true|false}`. Se implementa el contrato.
