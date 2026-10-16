#!/usr/bin/env bash
# Copyright 2026 Goldman Sachs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Validates that all source files (.rs, .toml) contain a copyright header.
# Only checks that "Copyright <YEAR>" exists — the entity name is up to the contributor.
# Exits with code 1 if any file is missing the header.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Pattern: "Copyright" followed by a 4-digit year, any entity is acceptable
PATTERN="Copyright [0-9]{4}"

MISSING=()
TOTAL=0

# Check .rs files under crates/
while IFS= read -r file; do
    TOTAL=$((TOTAL + 1))
    if ! head -n 5 "$file" | grep -qE "$PATTERN"; then
        MISSING+=("$file")
    fi
done < <(find "$PROJECT_ROOT/crates" -name "*.rs" -type f)

# Check .toml files (Cargo.toml, rustfmt.toml, config.toml, etc.)
while IFS= read -r file; do
    # Skip files under target/
    if [[ "$file" == */target/* ]]; then
        continue
    fi
    TOTAL=$((TOTAL + 1))
    if ! head -n 5 "$file" | grep -qE "$PATTERN"; then
        MISSING+=("$file")
    fi
done < <(find "$PROJECT_ROOT" -name "*.toml" -type f)

# Check .pure files (test corpus, examples)
while IFS= read -r file; do
    # Skip files under target/
    if [[ "$file" == */target/* ]]; then
        continue
    fi
    TOTAL=$((TOTAL + 1))
    if ! head -n 5 "$file" | grep -qE "$PATTERN"; then
        MISSING+=("$file")
    fi
done < <(find "$PROJECT_ROOT" -name "*.pure" -type f)

# Check .sh files (scripts)
while IFS= read -r file; do
    TOTAL=$((TOTAL + 1))
    if ! head -n 5 "$file" | grep -qE "$PATTERN"; then
        MISSING+=("$file")
    fi
done < <(find "$PROJECT_ROOT/scripts" -name "*.sh" -type f)

# NOTE: .md files are exempted from copyright headers by project policy.
# See CONTRIBUTING.md for details.

if [ ${#MISSING[@]} -gt 0 ]; then
    echo "ERROR: The following files are missing a copyright header:"
    echo ""
    for f in "${MISSING[@]}"; do
        echo "  - ${f#$PROJECT_ROOT/}"
    done
    echo ""
    echo "Expected pattern in the first 5 lines: Copyright <YEAR> <Your Name/Org>"
    echo ""
    echo "For .rs files, use // comments. For .toml and .sh files, use # comments."
    echo ""
    echo "Example (.rs):"
    echo "  // Copyright $(date +%Y) <Your Name or Organization>"
    echo "  // Licensed under the Apache License, Version 2.0 ..."
    echo ""
    echo "Example (.toml):"
    echo "  # Copyright $(date +%Y) <Your Name or Organization>"
    echo "  # Licensed under the Apache License, Version 2.0 ..."
    exit 1
fi

echo "✅ All $TOTAL source files (.rs, .toml, .pure, .sh) have copyright headers."
