#!/usr/bin/env bash
# Daily database backup. Keeps the last 14 days.
# Run from the project folder on the server:  bash deploy/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
mkdir -p backups
FILE="backups/aerosaga-$(date +%F-%H%M).sql.gz"
docker compose exec -T postgres pg_dump -U "$DB_USER" aerosaga | gzip > "$FILE"
# Refuse to keep an empty backup
if [ "$(gzip -dc "$FILE" | wc -c)" -lt 100 ]; then
  echo "Backup looks empty: $FILE" >&2
  exit 1
fi
find backups -name 'aerosaga-*.sql.gz' -mtime +14 -delete
echo "Backup written: $FILE"
