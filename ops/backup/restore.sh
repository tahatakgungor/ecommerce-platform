#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_FILE="${CONF_FILE:-$SCRIPT_DIR/backup.conf}"

usage() {
  cat <<EOF
Kullanım:
  $0 <backup_dir> [--db] [--volumes] [--yes]

Örnek:
  $0 /var/backups/ecommerce-platform/2026/04/12/backup_srv_2026-04-12T02-00-00 --db --volumes --yes
EOF
}

[[ $# -lt 1 ]] && { usage; exit 1; }
BACKUP_DIR="$1"; shift

RESTORE_DB=0
RESTORE_VOLUMES=0
ASSUME_YES=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db) RESTORE_DB=1 ;;
    --volumes) RESTORE_VOLUMES=1 ;;
    --yes) ASSUME_YES=1 ;;
    *) echo "Bilinmeyen parametre: $1"; usage; exit 1 ;;
  esac
  shift
done

if [[ $RESTORE_DB -eq 0 && $RESTORE_VOLUMES -eq 0 ]]; then
  echo "--db veya --volumes en az biri seçilmeli."
  exit 1
fi

if [[ ! -d "$BACKUP_DIR" ]]; then
  echo "Backup dizini yok: $BACKUP_DIR"
  exit 1
fi

if [[ ! -f "$CONF_FILE" ]]; then
  echo "Konfig bulunamadı: $CONF_FILE"
  exit 1
fi

# shellcheck disable=SC1090
source "$CONF_FILE"

if [[ $ASSUME_YES -ne 1 ]]; then
  echo "UYARI: Restore veri kaybına yol açabilir."
  echo "Önce uygulama container'larını durdurmanız önerilir."
  read -r -p "Devam edilsin mi? (yes/no): " answer
  [[ "$answer" == "yes" ]] || exit 1
fi

if [[ $RESTORE_DB -eq 1 ]]; then
  DB_DUMP="${BACKUP_DIR}/db_dump.sql.gz"
  GLOBALS_DUMP="${BACKUP_DIR}/db_globals.sql.gz"

  [[ -f "$DB_DUMP" ]] || { echo "DB dump bulunamadı: $DB_DUMP"; exit 1; }

  if [[ "${RECREATE_DB_ON_RESTORE:-0}" == "1" ]]; then
    echo "[DB] public schema sıfırlanıyor..."
    docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -v ON_ERROR_STOP=1 \
      -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
  fi

  if [[ -f "$GLOBALS_DUMP" ]]; then
    echo "[DB] global roller/izinler restore ediliyor..."
    gunzip -c "$GLOBALS_DUMP" | docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -v ON_ERROR_STOP=1
  fi

  echo "[DB] veritabanı restore ediliyor..."
  gunzip -c "$DB_DUMP" | docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -v ON_ERROR_STOP=1
fi

if [[ $RESTORE_VOLUMES -eq 1 ]]; then
  shopt -s nullglob
  volume_archives=("${BACKUP_DIR}"/volume_*.tar.gz)
  shopt -u nullglob

  if (( ${#volume_archives[@]} == 0 )); then
    echo "Volume arşivi bulunamadı: ${BACKUP_DIR}/volume_*.tar.gz"
    exit 1
  fi

  for archive in "${volume_archives[@]}"; do
    file="$(basename "$archive")"
    volume_name="${file#volume_}"
    volume_name="${volume_name%.tar.gz}"
    echo "[VOL] ${volume_name} restore ediliyor..."

    docker run --rm \
      -v "${volume_name}:/volume" \
      -v "${BACKUP_DIR}:/backup:ro" \
      alpine:3.20 \
      sh -lc "rm -rf /volume/* /volume/.[!.]* /volume/..?* 2>/dev/null || true; tar -xzf \"/backup/${file}\" -C /volume"
  done
fi

echo "Restore tamam."
