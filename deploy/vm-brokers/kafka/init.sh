#!/bin/sh
# Crea los topics y las ACL del proyecto. Es idempotente.
# Se conecta al listener LOCAL de kafka (principal ANONYMOUS = super usuario).
set -eu

B=127.0.0.1:9094
BIN=/opt/kafka/bin

topic() { # nombre particiones [config]
  "$BIN/kafka-topics.sh" --bootstrap-server "$B" --create --if-not-exists \
    --topic "$1" --partitions "$2" --replication-factor 1 ${3:+--config "$3"}
}

# principal topic operaciones...
topic_acl() {
  p=$1; t=$2; shift 2
  for op in "$@"; do
    "$BIN/kafka-acls.sh" --bootstrap-server "$B" --add --allow-principal "User:$p" \
      --operation "$op" --topic "$t"
  done
}

# principal grupo [prefixed]
group_acl() {
  "$BIN/kafka-acls.sh" --bootstrap-server "$B" --add --allow-principal "User:$1" \
    --operation Read --group "$2" ${3:+--resource-pattern-type prefixed}
}

topic eligible_voters 1 cleanup.policy=compact
topic raw_votes 3
topic security_alerts 1
topic security_drills 1   # pruebas controladas de detección (no contaminan raw_votes)
topic fraud_events 1      # voto duplicado / región incorrecta detectados por voting_api

# Productores
topic_acl voter_registration_service eligible_voters Write Describe
topic_acl voting_api                 raw_votes       Write Describe
topic_acl voting_api                 eligible_voters Write Describe   # altas en el padrón desde el panel de administración
topic_acl voting_api                 security_drills Write Describe   # lanza pruebas desde el panel
topic_acl voting_api                 fraud_events    Write Describe   # reporta incidentes electorales
topic_acl bot_detector_service       security_alerts Write Describe

# Consumidores
topic_acl user_validation_service eligible_voters Read Describe
group_acl user_validation_service user-validation-service- prefixed   # group.id lleva un UUID

topic_acl vote_processor          raw_votes Read Describe
group_acl vote_processor          vote-processor-group

topic_acl regional_rollup_service raw_votes Read Describe
group_acl regional_rollup_service regional-rollup-group

topic_acl bot_detector_service    raw_votes Read Describe
group_acl bot_detector_service    bot-detector-group

topic_acl bot_detector_service    security_drills Read Describe
group_acl bot_detector_service    bot-detector-drills

topic_acl bot_detector_service    fraud_events Read Describe
group_acl bot_detector_service    bot-detector-fraud

topic_acl analytics_archiver      raw_votes Read Describe
group_acl analytics_archiver      analytics-archiver

echo "kafka-init: topics y ACL listos"
