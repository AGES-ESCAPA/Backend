#!/bin/sh
set -eu

MINIO_HOST="${MINIO_HOST:-http://minio:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-escapa}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-escapa12345}"
BUCKET="${MINIO_BUCKET:-escapa-media}"

echo "Aguardando MinIO em ${MINIO_HOST}..."
i=0
until mc alias set local "${MINIO_HOST}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"; do
  i=$((i + 1))
  if [ "$i" -ge 30 ]; then
    echo "MinIO nao ficou pronto a tempo"
    exit 1
  fi
  echo "Tentativa ${i}/30 — MinIO ainda nao responde"
  sleep 2
done

mc mb "local/${BUCKET}" || true
mc anonymous set download "local/${BUCKET}"

if [ -f /seed/cors.json ]; then
  mc cors set "local/${BUCKET}" /seed/cors.json || true
fi

mc cp --recursive /seed/courses/ "local/${BUCKET}/courses/"
mc cp --recursive /seed/avatars/ "local/${BUCKET}/avatars/"
echo "Seed do MinIO concluido"
mc ls "local/${BUCKET}/courses/"
mc ls "local/${BUCKET}/avatars/"
