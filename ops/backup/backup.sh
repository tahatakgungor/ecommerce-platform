#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_FILE="${1:-$SCRIPT_DIR/backup.conf}"

if [[ ! -f "$CONF_FILE" ]]; then
  echo "Konfig bulunamadı: $CONF_FILE"
  echo "Önce şunu yapın:"
  echo "  cp $SCRIPT_DIR/backup.conf.example $SCRIPT_DIR/backup.conf"
  exit 1
fi

# shellcheck disable=SC1090
source "$CONF_FILE"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Eksik komut: $1"
    exit 1
  }
}

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Eksik konfig değişkeni: $name"
    exit 1
  fi
}

require_cmd docker
require_cmd gzip
require_cmd tar
require_cmd sha256sum

require_var BACKUP_ROOT
require_var PG_CONTAINER
require_var PG_USER
require_var PG_DB

TS="$(date '+%Y-%m-%dT%H-%M-%S')"
DATE_PATH="$(date '+%Y/%m/%d')"
HOST="$(hostname -s 2>/dev/null || echo server)"
BACKUP_NAME="backup_${HOST}_${TS}"
TARGET_DIR="${BACKUP_ROOT}/${DATE_PATH}/${BACKUP_NAME}"

mkdir -p "$TARGET_DIR"

echo "[1/6] PostgreSQL dump alınıyor..."
docker exec "$PG_CONTAINER" sh -lc \
  "pg_dump -U \"$PG_USER\" -d \"$PG_DB\" --no-owner --no-privileges --clean --if-exists" \
  | gzip -9 > "${TARGET_DIR}/db_dump.sql.gz"

echo "[2/6] Global rol/izin dump alınıyor..."
docker exec "$PG_CONTAINER" sh -lc \
  "pg_dumpall -U \"$PG_USER\" --globals-only" \
  | gzip -9 > "${TARGET_DIR}/db_globals.sql.gz"

echo "[3/6] Docker volume yedekleri alınıyor..."
for vol in ${DOCKER_VOLUMES:-}; do
  [[ -z "$vol" ]] && continue
  echo "  - volume: $vol"
  docker run --rm \
    -v "${vol}:/volume:ro" \
    -v "${TARGET_DIR}:/backup" \
    alpine:3.20 \
    sh -lc "tar -czf \"/backup/volume_${vol}.tar.gz\" -C /volume ."
done

echo "[4/6] Ek klasör yedekleri alınıyor..."
if [[ -n "${EXTRA_PATHS:-}" ]]; then
  mapfile -t existing_paths < <(
    for p in ${EXTRA_PATHS}; do
      [[ -e "$p" ]] && echo "$p"
    done
  )

  if (( ${#existing_paths[@]} > 0 )); then
    tar -czf "${TARGET_DIR}/extra_paths.tar.gz" -P "${existing_paths[@]}"
  else
    echo "  - Ek klasör bulunamadı, atlandı."
  fi
else
  echo "  - EXTRA_PATHS boş, atlandı."
fi

echo "[5/6] Checksum ve metadata yazılıyor..."
(
  cd "$TARGET_DIR"
  sha256sum ./* > SHA256SUMS
)
cat > "${TARGET_DIR}/metadata.txt" <<EOF
timestamp=${TS}
host=${HOST}
pg_container=${PG_CONTAINER}
pg_db=${PG_DB}
docker_volumes=${DOCKER_VOLUMES:-}
extra_paths=${EXTRA_PATHS:-}
EOF

echo "[6/6] Offsite kopya + local retention..."
if [[ "${ENABLE_RCLONE:-0}" == "1" ]]; then
  require_cmd rclone
  require_var RCLONE_REMOTE
  rclone copy "${TARGET_DIR}" "${RCLONE_REMOTE}/${DATE_PATH}/${BACKUP_NAME}" --create-empty-src-dirs
  echo "  - Offsite kopya tamam."
else
  echo "  - Offsite kapalı."
fi

if [[ -n "${RETENTION_DAYS_LOCAL:-}" ]]; then
  find "${BACKUP_ROOT}" -type d -name 'backup_*' -mtime +"${RETENTION_DAYS_LOCAL}" -print -exec rm -rf {} +
  find "${BACKUP_ROOT}" -type d -empty -delete
fi

echo "Yedek tamam: ${TARGET_DIR}"
