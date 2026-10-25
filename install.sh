#!/usr/bin/env bash
set -e
echo "Installing oma-cli to /usr/local/bin/oma-cli..."
sudo cp oma-cli /usr/local/bin/oma-cli || sudo curl -sSL https://raw.githubusercontent.com/mrstephenhanson/OmaLauncher-Companion/main/oma-cli -o /usr/local/bin/oma-cli
sudo chmod +x /usr/local/bin/oma-cli
echo "Done! oma-cli is installed."
oma-cli
