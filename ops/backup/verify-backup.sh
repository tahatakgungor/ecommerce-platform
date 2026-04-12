#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_FILE="${CONF_FILE:-$SCRIPT_DIR/backup.conf}"

usage() {
  cat <<EOF
Kullanım:
  $0 <backup_dir>
EOF
}

[[ $# -eq 1 ]] || { usage; exit 1; }
BACKUP_DIR="$1"
[[ -d "$BACKUP_DIR" ]] || { echo "Backup dizini yok: $BACKUP_DIR"; exit 1; }
[[ -f "$CONF_FILE" ]] || { echo "Konfig yok: $CONF_FILE"; exit 1; }

# shellcheck disable=SC1090
source "$CONF_FILE"

echo "[1/4] SHA256 doğrulaması..."
if [[ -f "${BACKUP_DIR}/SHA256SUMS" ]]; then
  (
    cd "$BACKUP_DIR"
    sha256sum -c SHA256SUMS
  )
else
  echo "SHA256SUMS bulunamadı, atlandı."
fi

echo "[2/4] Gzip arşiv testleri..."
for f in "${BACKUP_DIR}"/*.gz; do
  [[ -f "$f" ]] || continue
  gzip -t "$f"
done

if [[ "${VERIFY_WITH_TEMP_DB:-1}" != "1" ]]; then
  echo "Geçici DB restore testi kapalı."
  echo "Verify tamam."
  exit 0
fi

DB_DUMP="${BACKUP_DIR}/db_dump.sql.gz"
[[ -f "$DB_DUMP" ]] || { echo "db_dump.sql.gz yok, DB verify atlandı."; exit 0; }

TMP_NAME="backup-verify-$(date +%s)"
TMP_PASS="verifypass"
TMP_DB="verifydb"
TMP_USER="postgres"

echo "[3/4] Geçici PostgreSQL başlatılıyor..."
docker run -d --rm --name "$TMP_NAME" \
  -e POSTGRES_PASSWORD="$TMP_PASS" \
  -e POSTGRES_DB="$TMP_DB" \
  "${VERIFY_TEMP_DB_IMAGE:-postgres:15-alpine}" >/dev/null

cleanup() {
  docker rm -f "$TMP_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for _ in {1..30}; do
  if docker exec "$TMP_NAME" pg_isready -U "$TMP_USER" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo "[4/4] Dump restore testi..."
gunzip -c "$DB_DUMP" | docker exec -i "$TMP_NAME" psql -U "$TMP_USER" -d "$TMP_DB" -v ON_ERROR_STOP=1 >/dev/null

TABLE_COUNT="$(docker exec -i "$TMP_NAME" psql -U "$TMP_USER" -d "$TMP_DB" -tAc "select count(*) from information_schema.tables where table_schema='public';")"
echo "Restore OK. public tablo sayısı: ${TABLE_COUNT}"
echo "Verify tamam."
