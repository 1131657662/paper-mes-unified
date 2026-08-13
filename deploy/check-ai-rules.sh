#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RULES_FILE="${1:-$ROOT_DIR/src/main/resources/ai/rules-v1.0.0.json}"

cd "$ROOT_DIR"
sh mvnw -q -Dtest=AiRuleArtifactContractTest \
  -Dpaper.mes.ai.rules.file="$RULES_FILE" test
