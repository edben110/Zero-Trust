# Contratos entre servicios

> Pendiente de acordar con todo el equipo. Nada se implementa contra un valor que no esté aquí.

## Decisiones
- Estados del bot_detector: (NORMAL | CONFIABLE) / SOSPECHOSA / BLOQUEADA → pendiente
- Un voto por votante: el agregado de vote_processor ignora voterId repetido + UNIQUE(voter_id) en BD → pendiente
- Réplicas: analytics_archiver, user_validation_service, dashboard workers. vote_processor y regional_rollup_service con instancia única → pendiente

## Servicios
| Servicio | VM | Puerto | Jar (finalName) | client_id |
|---|---|---|---|---|

## Variables de entorno comunes
AUTH_TOKEN_URL, AUTH_JWKS_URL, CLIENT_ID, CLIENT_SECRET, KAFKA_BOOTSTRAP, RABBIT_HOST, RABBIT_PORT, DB_URL, DB_USER, DB_PASSWORD

## Token JWT (auth_server)
- POST /token, OAuth2 client_credentials (form-urlencoded; credenciales por Basic o en el body)
- Respuesta: {access_token, token_type: "Bearer", expires_in}
- RS256, JWKS en /.well-known/jwks.json
- Claims: iss, sub (= client_id), aud ["kafka","rabbitmq"], scope, iat, exp

## Kafka
| Topic | Productor | Consumidor | Key | Payload (JSON) |
|---|---|---|---|---|
| eligible_voters (compactado) | voter_registration_service | user_validation_service | | |
| raw_votes | voting_api | vote_processor, regional_rollup_service, bot_detector_service, analytics_archiver | | |
| security_alerts | bot_detector_service | | | |

## RabbitMQ
| Nombre | Tipo | Productor | Consumidor | Payload |
|---|---|---|---|---|
| user_validation_queue | cola RPC (reply-to + correlationId) | voting_api | user_validation_service | |
| live_results_global | fanout | vote_processor | global_dashboard_worker | |
| live_results_regional | topic | regional_rollup_service | regional_dashboard_worker | |

## API pública
POST /vote → payload pendiente (debe incluir lo necesario para la región; la IP la toma voting_api de la petición)
