#!/usr/bin/env bash
# =============================================================================
# kafka-acl-setup.sh
# =============================================================================
# Run this script on your Kafka broker to set up SCRAM-SHA-512 users and
# topic-level ACLs following the principle of least privilege.
#
# Prerequisites:
#   1. Kafka broker running with KRaft + SASL_SSL configured
#   2. kafka-configs and kafka-acls tools available on PATH
#   3. A superuser (e.g. 'admin') credentials in KAFKA_ADMIN_SASL_JAAS
#
# Usage:
#   BOOTSTRAP=kafka:9092 ADMIN_USER=admin ADMIN_PASS=<secret> bash kafka-acl-setup.sh
# =============================================================================

set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka:9092}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:?ADMIN_PASS env var required}"

ADMIN_OPTS=(
  --bootstrap-server "${BOOTSTRAP}"
  --command-config <(echo "
security.protocol=SASL_PLAINTEXT
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${ADMIN_USER}\" password=\"${ADMIN_PASS}\";
")
)

# ── Create SCRAM-SHA-512 users ───────────────────────────────────────────────
create_user() {
  local USER=$1 PASS=$2
  kafka-configs "${ADMIN_OPTS[@]}" \
    --entity-type users --entity-name "${USER}" \
    --alter --add-config "SCRAM-SHA-512=[password=${PASS}]"
  echo "Created SCRAM user: ${USER}"
}

create_user "api-gateway"         "${API_GATEWAY_KAFKA_PASS:?}"
create_user "market-access"       "${MARKET_ACCESS_KAFKA_PASS:?}"
create_user "contract-farming"    "${CONTRACT_FARMING_KAFKA_PASS:?}"
create_user "generate-agreement"  "${GENERATE_AGREEMENT_KAFKA_PASS:?}"
create_user "notification-svc"    "${NOTIFICATION_SVC_KAFKA_PASS:?}"

# ── Grant ACLs ───────────────────────────────────────────────────────────────
# Each producer: WRITE + DESCRIBE on its own topic only.
# Notification-Service: READ + DESCRIBE on all 4 notification topics + WRITE on DLQ.

grant_producer() {
  local USER=$1 TOPIC=$2
  kafka-acls "${ADMIN_OPTS[@]}" --add \
    --allow-principal "User:${USER}" \
    --operation Write --operation Describe --operation Create \
    --topic "${TOPIC}"
  echo "Granted WRITE to ${USER} on ${TOPIC}"
}

grant_consumer() {
  local USER=$1 TOPIC=$2 GROUP=$3
  kafka-acls "${ADMIN_OPTS[@]}" --add \
    --allow-principal "User:${USER}" \
    --operation Read --operation Describe \
    --topic "${TOPIC}"
  kafka-acls "${ADMIN_OPTS[@]}" --add \
    --allow-principal "User:${USER}" \
    --operation Read \
    --group "${GROUP}"
  echo "Granted READ to ${USER} on ${TOPIC} for group ${GROUP}"
}

grant_producer "api-gateway"        "agriconnect.notifications.auth"
grant_producer "market-access"      "agriconnect.notifications.market"
grant_producer "contract-farming"   "agriconnect.notifications.contract"
grant_producer "generate-agreement" "agriconnect.notifications.agreement"

# Notification Service reads all notification topics
TOPICS=("agriconnect.notifications.auth" "agriconnect.notifications.market"
        "agriconnect.notifications.contract" "agriconnect.notifications.agreement")
for TOPIC in "${TOPICS[@]}"; do
  grant_consumer "notification-svc" "${TOPIC}" "agriconnect-notification-service"
done

# Notification Service writes to DLQ
grant_producer "notification-svc" "agriconnect.notifications.dlq"

# DLQ reprocessor reads DLQ
grant_consumer "notification-svc" "agriconnect.notifications.dlq" "agriconnect-notification-dlq-reprocessor"

echo "All Kafka ACLs configured."
