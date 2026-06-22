#!/bin/bash
# One-time EC2 setup for development environment
# Run as: sudo bash ec2-setup-dev.sh
# Must be run AFTER ec2-setup.sh (Java already installed)
set -euo pipefail

APP_DIR="/opt/chem-os-dev"
SERVICE_NAME="chem-os-dev"
APP_USER="ec2-user"

echo "==> Creating dev app directory..."
mkdir -p "$APP_DIR/logs"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

echo "==> Installing dev deploy script..."
cp "$(dirname "$0")/deploy-dev.sh" "$APP_DIR/deploy-dev.sh"
chmod +x "$APP_DIR/deploy-dev.sh"
chown root:root "$APP_DIR/deploy-dev.sh"

echo "==> Creating empty env file (fill in secrets)..."
if [ ! -f "$APP_DIR/.env" ]; then
  cat > "$APP_DIR/.env" << 'EOF'
DB_URL=jdbc:postgresql://localhost:5432/chemos_dev
DB_USERNAME=chemos
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_TO_LONG_RANDOM_SECRET
EOF
  chmod 600 "$APP_DIR/.env"
  chown "$APP_USER:$APP_USER" "$APP_DIR/.env"
  echo "  !! Edit $APP_DIR/.env with real values before starting"
fi

echo "==> Installing systemd service..."
cp "$(dirname "$0")/chem-os-dev.service" "/etc/systemd/system/$SERVICE_NAME.service"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
echo "  Service enabled — will auto-start on reboot"

echo "==> Adding sudoers entry for deploy script..."
SUDOERS_FILE="/etc/sudoers.d/chem-os-dev-deploy"
echo "$APP_USER ALL=(ALL) NOPASSWD: /opt/chem-os-dev/deploy-dev.sh" > "$SUDOERS_FILE"
chmod 440 "$SUDOERS_FILE"

echo ""
echo "========================================="
echo "Dev setup complete. Next steps:"
echo "  1. Edit /opt/chem-os-dev/.env with real secrets"
echo "     (can reuse same DB as prod, or create chemos_dev DB)"
echo "  2. Add GitHub Secrets (see below)"
echo "  3. Push to development branch to trigger first deploy"
echo ""
echo "GitHub Secrets to add:"
echo "  DEV_DB_URL       — e.g. jdbc:postgresql://localhost:5432/chemos"
echo "  DEV_DB_USERNAME  — your postgres username"
echo "  DEV_DB_PASSWORD  — your postgres password"
echo "  DEV_JWT_SECRET   — any long random string"
echo "  (EC2_SSH_KEY, EC2_HOST, EC2_USER are shared with main — already set)"
echo "========================================="
