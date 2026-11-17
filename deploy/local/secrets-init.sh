#!/bin/sh
# Genera los secretos de DESARROLLO en el volumen /secrets. Solo para la prueba local.
# Es idempotente: si ya existen (.listo), no hace nada. Para regenerar: docker compose down -v.
set -eu
cd /secrets

if [ -f .listo ]; then
  echo "secrets-init: los secretos ya existen"
  exit 0
fi

apk add --no-cache openssl apache2-utils jq >/dev/null

P="${STORE_PASSWORD:?falta STORE_PASSWORD}"

# Clave de firma de los JWT: PKCS#8 PEM sin cifrar, RSA 2048.
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out firma.pem

# CA de desarrollo. Erlang (RabbitMQ) no acepta un certificado hoja autofirmado como ancla de
# confianza, asi que cada servidor usa un certificado firmado por esta CA.
keytool -genkeypair -alias ca -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore ca.p12 -storepass "$P" -keypass "$P" \
  -dname "CN=zerotrust-dev-ca" -ext bc:c -ext ku:c=keyCertSign,cRLSign
keytool -exportcert -rfc -alias ca -keystore ca.p12 -storepass "$P" -file ca.pem

# alias keystore CN SAN. auth_server solo admite UNA entrada con clave en su keystore;
# la CA entra como certificado de confianza, no como clave.
emitir() {
  keytool -genkeypair -alias "$1" -keyalg RSA -keysize 2048 -validity 365 \
    -storetype PKCS12 -keystore "$2" -storepass "$P" -keypass "$P" -dname "CN=$3" -ext "SAN=$4"
  keytool -certreq -alias "$1" -keystore "$2" -storepass "$P" -file "$1.csr" -ext "SAN=$4"
  keytool -gencert -alias ca -keystore ca.p12 -storepass "$P" -infile "$1.csr" -outfile "$1.pem" \
    -rfc -validity 365 -ext "SAN=$4"
  keytool -importcert -noprompt -alias ca -file ca.pem -keystore "$2" -storepass "$P"
  keytool -importcert -noprompt -alias "$1" -file "$1.pem" -keystore "$2" -storepass "$P"
}
emitir auth  tls.p12            vm-auth "dns:vm-auth,dns:auth-server,dns:localhost"
emitir kafka kafka.keystore.p12 kafka   "dns:kafka,dns:localhost"

# Truststore compartido por todos los clientes: solo la CA.
keytool -importcert -noprompt -alias ca -file ca.pem -keystore truststore.p12 \
  -storetype PKCS12 -storepass "$P"

# La clave de la CA no se conserva.
rm -f ca.p12 ./*.csr
# clientes.json: mismos clientes y scopes que el ejemplo, con el BCrypt (coste 12) de cada secreto.
HASHES='{}'
for id in $(jq -r '.clients[].client_id' /clientes.ejemplo.json); do
  var="SECRET_$(echo "$id" | tr 'a-z' 'A-Z')"
  secret="$(printenv "$var")" || { echo "secrets-init: falta $var" >&2; exit 1; }
  hash="$(htpasswd -bnBC 12 x "$secret" | cut -d: -f2 | tr -d '\n')"
  HASHES="$(echo "$HASHES" | jq --arg k "$id" --arg h "$hash" '. + {($k): $h}')"
done
# Ajuste SOLO de la prueba local: user_validation_service declara su cola al arrancar y el
# ejemplo solo le da read sobre ella. Pendiente de acordar los scopes reales en docs/contratos.md.
jq --argjson h "$HASHES" '
  .clients |= map(.secret_hash = $h[.client_id])
  | (.clients[] | select(.client_id == "user_validation_service") | .scopes)
      += ["rabbitmq.configure:votacion/user_validation_queue"]' \
  /clientes.ejemplo.json > clientes.json

# Los contenedores corren con distintos usuarios no privilegiados: lectura para todos.
chmod 755 /secrets
chmod 644 ./*.pem ./*.p12 ./*.json
touch .listo
echo "secrets-init: secretos generados"
