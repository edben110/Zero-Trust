# Contratos entre servicios

> Pendiente de acordar con todo el equipo. Nada se implementa contra un valor que no esté aquí.

> **Propuesto por Sebas, pendiente de validar con el equipo:** fila y jar de auth_server, convención de finalName, "Variables de entorno de auth_server", aud por cliente en "Token JWT", "RabbitMQ OAuth2", "Tráfico hacia vm-auth:443" y el refresco de token de los clientes Kafka.

## Decisiones
- Estados del bot_detector: **NORMAL / SOSPECHOSA / BLOQUEADA** (propuesto Daniel: se usa NORMAL como estado inicial, alineado con el enunciado)
- Un voto por votante: el agregado de vote_processor ignora voterId repetido + UNIQUE(voter_id) en BD → pendiente validar con equipo
- Réplicas: analytics_archiver ×2, user_validation_service ×2, global_dashboard_worker ×2, regional_dashboard_worker ×2. vote_processor y regional_rollup_service instancia única (stateful)

## Servicios
| Servicio | VM | Puerto | Jar (finalName) | client_id |
|---|---|---|---|---|
| auth_server | vm-auth | 443 en la VM → 8443 en el contenedor (SERVER_PORT) | auth-server.jar | — (es el emisor) |

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
