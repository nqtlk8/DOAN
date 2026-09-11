#!/bin/bash
set -euo pipefail

# Directory for secrets
SECRETS_DIR="$(dirname "$0")/../secrets"
mkdir -p "$SECRETS_DIR"

# Generate Private Key
openssl genpkey -algorithm RSA -out "$SECRETS_DIR/private_key.pem" -pkeyopt rsa_keygen_bits:2048
# Convert to PKCS#8 format (Spring Boot requires PKCS#8)
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in "$SECRETS_DIR/private_key.pem" -out "$SECRETS_DIR/private_key_pkcs8.pem"
mv "$SECRETS_DIR/private_key_pkcs8.pem" "$SECRETS_DIR/private_key.pem"

# Generate Public Key
openssl rsa -pubout -in "$SECRETS_DIR/private_key.pem" -out "$SECRETS_DIR/public_key.pem"

echo "RSA Key pair generated in $SECRETS_DIR"
