#!/bin/bash
# One-time EC2 setup script
# Run as: sudo bash ec2-setup.sh
# Tested on Ubuntu 22.04 / 24.04
set -euo pipefail

APP_DIR="/opt/chem-os"
SERVICE_NAME="chem-os"
APP_USER="ubuntu"   # change to ec2-user if Amazon Linux

echo "==> Installing Java 21..."
apt-get update -q
apt-get install -y -q openjdk-21-jre-headless

echo "Java version: $(java -version 2>&1 | head -1)"

echo "==> Creating app directory..."
mkdir -p "$APP_DIR/logs"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

echo "==> Installing deploy script..."
# deploy.sh will be committed to git and copied here manually once
# Or you can curl it from your repo after this setup
cp "$(dirname "$0")/deploy.sh" "$APP_DIR/deploy.sh"
chmod +x "$APP_DIR/deploy.sh"
# deploy.sh must be owned by root but executable by GitHub Actions SSH user via sudo
chown root:root "$APP_DIR/deploy.sh"

echo "==> Creating empty env file (fill in secrets)..."
if [ ! -f "$APP_DIR/.env" ]; then
  cat > "$APP_DIR/.env" << 'EOF'
DB_URL=jdbc:postgresql://YOUR_RDS_HOST:5432/chemos
DB_USERNAME=chemos
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_TO_LONG_RANDOM_SECRET
EOF
  chmod 600 "$APP_DIR/.env"
  chown "$APP_USER:$APP_USER" "$APP_DIR/.env"
  echo "  !! Edit $APP_DIR/.env and fill in real secrets before starting the service"
fi

echo "==> Installing systemd service..."
cp "$(dirname "$0")/chem-os.service" "/etc/systemd/system/$SERVICE_NAME.service"
# Replace 'ubuntu' with actual user if needed
sed -i "s/User=ubuntu/User=$APP_USER/" "/etc/systemd/system/$SERVICE_NAME.service"
sed -i "s/Group=ubuntu/Group=$APP_USER/" "/etc/systemd/system/$SERVICE_NAME.service"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
echo "  Service enabled — will auto-start on reboot"

echo "==> Allowing ubuntu user to restart the service without password prompt..."
SUDOERS_FILE="/etc/sudoers.d/chem-os-deploy"
echo "$APP_USER ALL=(ALL) NOPASSWD: /opt/chem-os/deploy.sh" > "$SUDOERS_FILE"
chmod 440 "$SUDOERS_FILE"

echo ""
echo "========================================="
echo "Setup complete. Next steps:"
echo "  1. Edit /opt/chem-os/.env with real secrets"
echo "  2. Copy your JAR to /opt/chem-os/chem-os-current.jar"
echo "  3. sudo systemctl start $SERVICE_NAME"
echo "  4. sudo journalctl -u $SERVICE_NAME -f   (to watch logs)"
echo "  5. Add GitHub Secrets (see README or deploy.yml comments)"
echo "========================================="
