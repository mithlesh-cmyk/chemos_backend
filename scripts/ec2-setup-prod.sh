#!/bin/bash
# One-time EC2 setup for production environment
# Run as: sudo bash ec2-setup-prod.sh
# Must be run AFTER Java is installed on the instance
set -euo pipefail

APP_DIR="/opt/chem-os"
SERVICE_NAME="chem-os"
APP_USER="ec2-user"

echo "==> Creating prod app directory..."
mkdir -p "$APP_DIR/logs"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

echo "==> Installing prod deploy script..."
cp "$(dirname "$0")/deploy-prod.sh" "$APP_DIR/deploy-prod.sh"
chmod +x "$APP_DIR/deploy-prod.sh"
chown root:root "$APP_DIR/deploy-prod.sh"

echo "==> Creating empty env file (fill in secrets)..."
if [ ! -f "$APP_DIR/.env" ]; then
  cat > "$APP_DIR/.env" << 'EOF'
DB_URL=jdbc:postgresql://localhost:5432/chemos
DB_USERNAME=chemos
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_TO_LONG_RANDOM_SECRET
EOF
  chmod 600 "$APP_DIR/.env"
  chown "$APP_USER:$APP_USER" "$APP_DIR/.env"
  echo "  !! Edit $APP_DIR/.env with real values before starting"
fi

echo "==> Installing systemd service..."
cp "$(dirname "$0")/chem-os-prod.service" "/etc/systemd/system/$SERVICE_NAME.service"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
echo "  Service enabled — will auto-start on reboot"

echo "==> Adding sudoers entry for deploy script..."
SUDOERS_FILE="/etc/sudoers.d/chem-os-prod-deploy"
echo "$APP_USER ALL=(ALL) NOPASSWD: /opt/chem-os/deploy-prod.sh" > "$SUDOERS_FILE"
chmod 440 "$SUDOERS_FILE"

echo ""
echo "========================================="
echo "Prod setup complete. Next steps:"
echo "  1. Edit /opt/chem-os/.env with real secrets"
echo "  2. Add GitHub Secrets (see below)"
echo "  3. Push to main branch to trigger first deploy"
echo ""
echo "GitHub Secrets to add:"
echo "  PROD_DB_URL          — e.g. jdbc:postgresql://localhost:5432/chemos"
echo "  PROD_DB_USERNAME     — your postgres username"
echo "  PROD_DB_PASSWORD     — your postgres password"
echo "  PROD_JWT_SECRET      — any long random string"
echo "  PROD_ADMIN_USERNAME  — admin login username"
echo "  PROD_ADMIN_PASSWORD  — admin login password"
echo "  PROD_ADMIN_NAME      — admin display name"
echo "  PROD_ADMIN_EMAIL     — admin email"
echo "  (EC2_SSH_KEY, EC2_HOST, EC2_USER are shared with dev — already set)"
echo "========================================="
