#!/usr/bin/env bash
# ============================================================
#  AgriConnect — EC2 One-Time Setup Script
#
#  Run this ONCE on a fresh EC2 instance to install all
#  prerequisites and clone the repository so that the
#  GitHub Actions CI/CD pipeline can deploy to it.
#
#  Supported OS : Ubuntu 22.04 / 24.04 LTS  (recommended)
#                 Amazon Linux 2023
#
#  Usage
#  ─────
#  1. SSH into your EC2 instance:
#       ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
#
#  2. Download and run this script:
#       curl -fsSL https://raw.githubusercontent.com/Vandit-Kalathiya/AgriConnect-Backend/main/scripts/ec2-setup.sh | bash
#
#     Or copy it manually and run:
#       bash ec2-setup.sh
# ============================================================

set -euo pipefail

REPO_URL="https://github.com/Vandit-Kalathiya/AgriConnect-Backend.git"
APP_DIR="$HOME/AgriConnect_Backend"

# ── helpers ──────────────────────────────────────────────────────────
info()    { echo -e "\e[34m[INFO]\e[0m  $*"; }
success() { echo -e "\e[32m[OK]\e[0m    $*"; }
warn()    { echo -e "\e[33m[WARN]\e[0m  $*"; }
die()     { echo -e "\e[31m[ERROR]\e[0m $*" >&2; exit 1; }

# ── pre-flight checks ─────────────────────────────────────────────────
# Detect OS family
if   command -v apt-get &>/dev/null; then PKG_MANAGER="apt"
elif command -v yum     &>/dev/null; then PKG_MANAGER="yum"
else die "Unsupported OS — only Ubuntu/Debian and Amazon Linux are supported."; fi

# ──────────────────────────────────────────────────────────────────────
# STEP 1 · System update
# ──────────────────────────────────────────────────────────────────────
info "Updating package index…"
if [[ "$PKG_MANAGER" == "apt" ]]; then
  sudo apt-get update -y -q
else
  sudo yum update -y -q
fi
success "Package index updated."

# ──────────────────────────────────────────────────────────────────────
# STEP 2 · Install Docker Engine + Compose plugin
# ──────────────────────────────────────────────────────────────────────
if command -v docker &>/dev/null; then
  warn "Docker is already installed ($(docker --version)). Skipping."
else
  info "Installing Docker…"

  if [[ "$PKG_MANAGER" == "apt" ]]; then
    sudo apt-get install -y -q ca-certificates curl gnupg lsb-release

    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
      | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
      | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update -y -q
    sudo apt-get install -y -q \
      docker-ce docker-ce-cli containerd.io \
      docker-buildx-plugin docker-compose-plugin

  else
    # Amazon Linux 2023
    sudo yum install -y -q docker
    sudo systemctl enable docker
    sudo systemctl start docker
    # Docker Compose v2 plugin for Amazon Linux
    COMPOSE_VERSION="v2.24.7"
    sudo mkdir -p /usr/local/lib/docker/cli-plugins
    sudo curl -SL \
      "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
  fi

  # Add current user to the docker group so sudo is not required
  sudo usermod -aG docker "$USER"
  success "Docker installed."
fi

# Enable + start Docker daemon
sudo systemctl enable docker --quiet
sudo systemctl start  docker --quiet

# ──────────────────────────────────────────────────────────────────────
# STEP 3 · Install git (usually pre-installed but ensure it's present)
# ──────────────────────────────────────────────────────────────────────
if ! command -v git &>/dev/null; then
  info "Installing git…"
  if [[ "$PKG_MANAGER" == "apt" ]]; then
    sudo apt-get install -y -q git
  else
    sudo yum install -y -q git
  fi
  success "git installed."
else
  success "git is already installed ($(git --version))."
fi

# ──────────────────────────────────────────────────────────────────────
# STEP 4 · Clone the repository (or pull if it already exists)
# ──────────────────────────────────────────────────────────────────────
if [ -d "$APP_DIR/.git" ]; then
  info "Repository already cloned at $APP_DIR — pulling latest changes…"
  git -C "$APP_DIR" pull origin main
  success "Repository updated."
else
  info "Cloning repository into $APP_DIR…"
  git clone "$REPO_URL" "$APP_DIR"
  success "Repository cloned."
fi

# ──────────────────────────────────────────────────────────────────────
# STEP 5 · Create placeholder .env files
# The CI/CD pipeline will overwrite these with real secrets on every
# deployment. We create them now so docker compose doesn't error on
# the first manual test run.
# ──────────────────────────────────────────────────────────────────────
info "Creating placeholder .env files…"
cd "$APP_DIR"

touch .env
touch Main-Backend/.env
touch Contract-Farming-App/.env
touch Market-Access-App/.env
touch Generate-Agreement-App/.env

success "Placeholder .env files created."

# ──────────────────────────────────────────────────────────────────────
# STEP 6 · Open required firewall ports (ufw — Ubuntu only)
# ──────────────────────────────────────────────────────────────────────
if command -v ufw &>/dev/null; then
  info "Configuring ufw firewall rules…"
  sudo ufw allow 22/tcp    comment "SSH"        > /dev/null 2>&1 || true
  sudo ufw allow 8080/tcp  comment "API Gateway" > /dev/null 2>&1 || true
  sudo ufw allow 8761/tcp  comment "Eureka UI"   > /dev/null 2>&1 || true
  # Individual service ports — open only if you need direct access
  # (normally all traffic should go through the API Gateway on 8080)
  # sudo ufw allow 2525/tcp  comment "Main-Backend"
  # sudo ufw allow 2526/tcp  comment "Contract-Farming"
  # sudo ufw allow 2527/tcp  comment "Market-Access"
  # sudo ufw allow 2529/tcp  comment "Generate-Agreement"
  success "ufw rules applied."
else
  warn "ufw not found — open ports 22, 8080, and 8761 manually in your EC2 Security Group."
fi

# ──────────────────────────────────────────────────────────────────────
# STEP 7 · Verify
# ──────────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
success "EC2 setup complete!"
echo ""
echo "  Docker  : $(docker --version)"
echo "  Compose : $(docker compose version)"
echo "  Git     : $(git --version)"
echo "  App dir : $APP_DIR"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  NEXT STEPS"
echo "  ──────────"
echo "  1. Log out and log back in so the docker group takes effect:"
echo "       exit"
echo "       ssh -i your-key.pem $USER@<EC2-PUBLIC-IP>"
echo ""
echo "  2. Add the following secrets to your GitHub repository"
echo "     (Settings → Secrets and variables → Actions → New secret):"
echo ""
echo "     EC2_HOST              → $(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<EC2-PUBLIC-IP>')"
echo "     EC2_USER              → $USER"
echo "     EC2_SSH_KEY           → <paste the full contents of your .pem file>"
echo "     EC2_PORT              → 22  (optional)"
echo "     DOCKER_USERNAME       → <your Docker Hub username>"
echo "     DOCKER_PASSWORD       → <your Docker Hub access token>"
echo "     MYSQL_ROOT_PASSWORD   → <strong password>"
echo "     DB_USERNAME           → root"
echo "     DB_PASSWORD           → <same as MYSQL_ROOT_PASSWORD>"
echo "     GATEWAY_URL           → http://$(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo '<EC2-PUBLIC-IP>'):8080"
echo "     JWT_SECRET            → <min 32-char random string>"
echo "     TWILIO_ACCOUNT_SID    → <from Twilio console>"
echo "     TWILIO_AUTH_TOKEN     → <from Twilio console>"
echo "     TWILIO_PHONE_NUMBER   → <Twilio phone number>"
echo "     RAZORPAY_KEY_ID       → <from Razorpay dashboard>"
echo "     RAZORPAY_KEY_SECRET   → <from Razorpay dashboard>"
echo "     CONTRACT_ADDRESS      → <deployed smart-contract address>"
echo "     PRIVATE_KEY           → <blockchain wallet private key>"
echo "     BLOCKCHAIN_API_URL    → <RPC endpoint, e.g. Infura URL>"
echo "     MAIL_USERNAME         → <sender Gmail address>"
echo "     MAIL_PASS             → <Gmail app password>"
echo "     GOOGLE_MAP_API_KEY    → <Google Maps API key>"
echo ""
echo "  3. Push any commit to the main branch — the pipeline will"
echo "     build, push images, and deploy automatically."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
