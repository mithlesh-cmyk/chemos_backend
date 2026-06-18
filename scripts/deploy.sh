#!/bin/bash
# Deployment script — runs on EC2, called by GitHub Actions
# Place at: /opt/chem-os/deploy.sh  (sudo chmod +x, owned by root)
set -euo pipefail

APP_DIR="/opt/chem-os"
SERVICE_NAME="chem-os"
NEW_JAR="$APP_DIR/chem-os-new.jar"
CURRENT_JAR="$APP_DIR/chem-os-current.jar"
BACKUP_JAR="$APP_DIR/chem-os-backup.jar"
LOG_FILE="$APP_DIR/logs/deploy.log"

mkdir -p "$APP_DIR/logs"
exec >> "$LOG_FILE" 2>&1

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

log "========== DEPLOY START =========="

if [ ! -f "$NEW_JAR" ]; then
  log "ERROR: $NEW_JAR not found — nothing to deploy"
  exit 1
fi

# Backup current JAR
if [ -f "$CURRENT_JAR" ]; then
  cp "$CURRENT_JAR" "$BACKUP_JAR"
  log "Backed up current JAR"
fi

# Swap JAR
mv "$NEW_JAR" "$CURRENT_JAR"
log "New JAR placed as current"

# Fix permissions on env file
chmod 600 "$APP_DIR/.env" 2>/dev/null || true

# Restart service
log "Restarting $SERVICE_NAME service..."
systemctl restart "$SERVICE_NAME"

# Wait for app to come up (Spring Boot typically takes 10–15s)
sleep 15

# Verify service is running
if systemctl is-active --quiet "$SERVICE_NAME"; then
  log "Service is active — deploy SUCCESS"
  log "========== DEPLOY END =========="
  exit 0
fi

# Service failed — roll back
log "ERROR: Service not active after restart — rolling back"
if [ -f "$BACKUP_JAR" ]; then
  mv "$BACKUP_JAR" "$CURRENT_JAR"
  systemctl restart "$SERVICE_NAME"
  sleep 10
  if systemctl is-active --quiet "$SERVICE_NAME"; then
    log "Rollback SUCCESSFUL — previous version restored"
  else
    log "CRITICAL: Rollback failed — manual intervention required"
  fi
else
  log "CRITICAL: No backup JAR found — service is down"
fi

log "========== DEPLOY END (FAILED) =========="
exit 1
