# Contratos entre servicios

> Pendiente de acordar con todo el equipo. Nada se implementa contra un valor que no esté aquí.

> **Propuesto por Sebas, pendiente de validar con el equipo:** fila y jar de auth_server, convención de finalName, "Variables de entorno de auth_server", aud por cliente en "Token JWT", "RabbitMQ OAuth2", "Tráfico hacia vm-auth:443", el refresco de token de los clientes Kafka, las filas de voting_api, user_validation_service, voter_registration_service y analytics_archiver, "Cliente de token", "Catálogo", "Respuestas de POST /vote", "Variables de entorno de los servicios de Sebas y del front" y "Tráfico adicional".

## Decisiones
- Estados del bot_detector: **NORMAL / SOSPECHOSA / BLOQUEADA** (propuesto Daniel: se usa NORMAL como estado inicial, alineado con el enunciado)
- Un voto por votante: el agregado de vote_processor ignora voterId repetido + UNIQUE(voter_id) en BD → pendiente validar con equipo
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
| eligible_voters (compactado) | voter_registration_service | user_validation_service | voterId | `{"voterId":"<uuid>","fullName":"<string>","region":"<string>","registeredAt":"<ISO-8601>"}` |
| raw_votes | voting_api | vote_processor, regional_rollup_service, bot_detector_service, analytics_archiver | voteId | `{"voteId":"<uuid>","voterId":"<uuid>","candidateId":"<uuid>","region":"<string>","sourceIp":"<string>","timestamp":"<ISO-8601>"}` |
| security_alerts | bot_detector_service | — | ip | `{"ip":"<string>","riskLevel":"NORMAL\|SOSPECHOSA\|BLOQUEADA","voteCount":<int>,"windowSeconds":<int>,"detectedAt":"<ISO-8601>"}` |

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
| 200 | `{"voteId":"<uuid>","receivedAt":"<ISO-8601>"}` | Votante habilitado y voto publicado en `raw_votes` |
| 400 | `{"error":"invalid_request","fields":[...]}` | Payload inválido, candidato o región fuera del catálogo |
| 403 | `{"error":"voter_not_eligible"}` | user_validation_service respondió `{"valid":false}` |
| 503 | `{"error":"validation_unavailable"}` | Sin respuesta RPC en `RPC_TIMEOUT_MS` o broker caído |

`sourceIp` es la dirección remota de la petición. `X-Forwarded-For` solo se usa si `TRUST_FORWARDED_HEADERS=true` (detrás de un proxy de confianza), para que un cliente no pueda falsificar su IP ante bot_detector_service.

## Variables de entorno de los servicios de Sebas y del front
> Propuesto por Sebas, pendiente de validar con el equipo.

| Variable | Servicio | Descripción |
|---|---|---|
| CATALOG_FILE | voting_api | Ruta del JSON del catálogo |
| CORS_ALLOWED_ORIGINS | voting_api | Orígenes permitidos, separados por comas, sin comodín |
| TRUST_FORWARDED_HEADERS | voting_api | `true` solo detrás de un proxy de confianza (por defecto `false`) |
| RPC_TIMEOUT_MS | voting_api | Espera máxima de la respuesta RPC (por defecto 3000) |
| VOTERS_FILE | voter_registration_service | Ruta del JSON con el padrón inicial |
| NEXT_PUBLIC_VOTING_API_URL | front | URL base de voting_api |
| NEXT_PUBLIC_WS_GLOBAL_URL | front | URL STOMP del global_dashboard_worker |
| NEXT_PUBLIC_WS_REGIONAL_URL | front | URL STOMP del regional_dashboard_worker |

## Tráfico adicional (para nftables)
> Propuesto por Sebas, pendiente de validar con el equipo.

| Origen | Destino | Para qué |
|---|---|---|
| vm-init | vm-brokers:5432 | voter_registration_service guarda el padrón en `registered_voters` (`docs/schema.sql`) |
| navegador | vm-api (voting_api) | Tarjetón: `GET /catalog` y `POST /vote` |
| navegador | vm-app (dashboard workers) | WebSocket/STOMP de resultados en vivo |

## Puntos a confirmar
- El enunciado dice que la respuesta RPC es "valido"/"invalido"; el contrato vigente es `{"valid":true|false}`. Se implementa el contrato.
