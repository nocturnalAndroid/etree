#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Running JS UI smoke tests..."
node "$SCRIPT_DIR/ui-test.js"
