# Administración de candidatos

`/admin` usa el mismo catálogo persistente de `voting_api` que valida `POST /vote`.
El archivo inicial `config/catalogo.json` solo se copia en el primer arranque; los
cambios se guardan en el volumen Docker `catalog-data`, que sobrevive a reinicios
y reconstrucciones de contenedores. No elimine ese volumen al redesplegar.

## Variables privadas (en `.env`, nunca en Git)

- `vm-api`: `ADMIN_API_TOKEN`, aleatorio de al menos 32 caracteres.
- `vm-app`: el **mismo** `ADMIN_API_TOKEN`, `ADMIN_SESSION_SECRET` aleatorio
  de al menos 32 caracteres, `ADMIN_PASSWORD_HASH` (scrypt, no contraseña)
  y `VM_API_IP` apuntando a la IP privada de `vm-api` (el compose usa
  `ADMIN_API_URL=http://vm-api:80` dentro de la red interna).

## Nueva votación

El botón **Reiniciar votación** en `/admin` cambia la jornada activa de forma
transaccional. Conserva los candidatos y el padrón; cada votante puede volver a
votar una vez. Los resultados nacionales y regionales se consultan desde la
misma tabla `accepted_votes` filtrada por `election_state.run_id`. Los votos
anteriores no se muestran pero permanecen para auditoría; no se borran los
registros de `vote_audit`. Los cambios en `docs/schema.sql` se aplican
automáticamente al iniciar una base PostgreSQL **nueva**. En AWS,
`vm-brokers` aplica `docs/migrations/*.sql` en cada convergencia después de que
PostgreSQL esté listo; esos archivos deben ser idempotentes. En local, con un
volumen nuevo, basta `schema.sql`.

## Región registrada y anulación del voto

`POST /vote` compara la región elegida con `registered_voters.region` en el
servidor, dentro de la misma transacción del voto. El primer error devuelve 422
con el texto `error esa no es la region que ud designo` y deja un único intento;
el segundo devuelve 409 `vote_invalidated` y anula el voto en esa jornada.

Los fallos se guardan por `election_state.run_id` en `voter_region_attempts`, con
el UUID pseudónimo del votante; al iniciar otra votación cambia el `run_id` y
los intentos anteriores dejan de aplicar. No se guarda la cédula ni el nombre.

La migración `docs/migrations/20261118_region_attempts.sql` crea esa tabla y
elimina `registered_voters.full_name`; el borrado es irreversible, así que haga
respaldo si necesita esos datos. Para aplicarla a mano en una base existente:

```bash
./deploy/aws/zerotrust-aws.sh ssh vm-brokers
cd /opt/zerotrust/src/deploy/vm-brokers
docker compose --env-file .env exec -T postgres-db \
  sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < /opt/zerotrust/src/docs/migrations/20261118_region_attempts.sql
```

## Panel administrativo

La conexión administrativa va de **vm-app (servidor Next)** a **vm-api:80**
por la red privada; no es vm-api quien llama al panel. El SG de vm-app permite
salida a vm-api:80 y el de vm-api permite entrada desde vm-app; las reglas
nftables permiten salida hacia la IP privada de vm-api y entrada en vm-api:80.
El panel se abre solo por túnel SSM a `http://localhost:3001/admin`.
El script AWS genera ambos secretos aleatorios y crea el hash a partir de
`ADMIN_PASSWORD_FILE` fuera de Git. El proxy público en `:3000` devuelve 404
para `/admin` y `/admin/*`; el front original solo publica `127.0.0.1:3001`
en el host, y los Security Groups no abren `3001`.
Ese proxy es `nginx:1.27-alpine` ejecutado como uid `101` (sin privilegios)
con `cap_drop: ALL`: los `tmpfs` de `/var/cache/nginx` y `/var/run` se montan
con `uid=101,gid=101` porque, si nginx arranca como root sin capacidades, el
master no puede hacer `chown` de sus directorios temporales y entra en bucle
de reinicio.

En otra terminal, con las mismas credenciales AWS utilizadas para desplegar:

```bash
./deploy/aws/zerotrust-aws.sh admin-tunnel
```

Equivalente con AWS CLI:

```bash
aws ssm start-session --region us-east-1 --target ID_DE_VM_APP \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["3001"],"localPortNumber":["3001"]}'
```

Se necesita `session-manager-plugin` en el equipo local. Abra la URL
`http://localhost:3001/admin` mientras la sesión SSM siga abierta.

El usuario es `admin`. La contraseña nunca se publica con `NEXT_PUBLIC_`.
Las peticiones de modificación pasan por el servidor Next.js y exigen una
sesión firmada, cookie HttpOnly/SameSite y comprobación de origen. El token
interno no se envía al navegador. `voting_api` valida ese token también.

## Protección del acceso por túnel

HTTP **no cifra** la contraseña ni la cookie de sesión por sí solo. El túnel
SSM protege el transporte fuera del equipo local; el navegador propio siempre
puede mostrar la contraseña enviada en su pestaña Network. No acceda al panel
por IP pública ni envíe credenciales fuera del túnel.
El hash scrypt protege únicamente el almacenamiento en el contenedor; cinco
fallos bloquean el login durante quince minutos (por proceso).
El token de `vm-api` no llega al navegador, pero viaja sin cifrar en la red
privada de AWS entre `vm-app` y `vm-api`.

El BFF de `vm-app` necesita acceso a `vm-api:80` por la red privada, con reglas
de salida/entrada en ambos Security Groups y nftables; el orquestador AWS
incluye estas reglas, pero **las instancias existentes requieren aplicar la
actualización de infraestructura** antes de usar la nueva ruta.

Se impide borrar un candidato si quedarían menos de dos; si el catálogo
inicial solo tiene uno, `/vote` devuelve 503 y la página advierte antes de
salir. El tarjetón consulta el catálogo cada 2 segundos para reflejar
ediciones de otros dispositivos. Cambiar candidatos después de emitir votos
puede afectar la presentación del escrutinio histórico: para una elección
real se necesita además un ciclo de publicación/congelación del catálogo.
