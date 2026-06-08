#!/usr/bin/env bash
set -euo pipefail

REPO="${1:-}"
if [ -z "$REPO" ]; then
  echo "Usage: $0 <github-repo-url>"
  echo "  e.g. $0 https://github.com/youruser/canary"
  exit 1
fi

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

echo "Cloning $REPO ..."
git clone --depth 1000 "$REPO" "$WORKDIR" 2>/dev/null || {
  git clone "$REPO" "$WORKDIR"
}
cd "$WORKDIR"

if [ ! -d canary ]; then
  echo "No canary/ directory found in repo."
  exit 1
fi

FILES=$(ls canary/canary-*.txt 2>/dev/null | sort)
COUNT=0
PREV_HASH=""
BREAK=0

if [ -z "$FILES" ]; then
  echo "No canary files found."
  exit 1
fi

echo ""
echo "=== Canary Chain Verification ==="
echo ""

for f in $FILES; do
  COUNT=$((COUNT + 1))
  FILE_HASH=$(sha256sum "$f" | cut -d' ' -f1)
  CLAIMED_PREV=$(grep "Previous Hash" "$f" | sed 's/.*SHA256): //' || echo "")
  BASENAME=$(basename "$f")

  echo "[$COUNT] $BASENAME"

  # GPG signature check
  if [ -f "${f}.asc" ]; then
    if gpg --verify "${f}.asc" "$f" 2>/dev/null; then
      echo "  GPG: OK"
    else
      echo "  GPG: FAILED"
      BREAK=1
    fi
  else
    echo "  GPG: MISSING (.asc not found)"
    BREAK=1
  fi

  # Hash chain continuity
  if [ "$COUNT" -gt 1 ]; then
    if [ "$CLAIMED_PREV" = "$PREV_HASH" ]; then
      echo "  Chain: OK"
    else
      echo "  Chain: BREAK"
      echo "    expected: $PREV_HASH"
      echo "    got:      $CLAIMED_PREV"
      BREAK=1
    fi
  else
    echo "  Chain: genesis"
  fi

  # Signed git tag
  TAG="canary-$(echo "$BASENAME" | sed 's/canary-//; s/\.txt$//')"
  if git tag -v "$TAG" 2>/dev/null; then
    echo "  Tag: OK (signed)"
  elif git tag -l "$TAG" | grep -q .; then
    echo "  Tag: EXISTS (unsigned)"
  else
    echo "  Tag: MISSING"
    BREAK=1
  fi

  # OTS proof exists
  if [ -f "${f}.ots" ]; then
    if command -v ots &>/dev/null; then
      if ots verify "${f}.ots" 2>/dev/null; then
        echo "  OTS: VERIFIED (Bitcoin anchor)"
      else
        echo "  OTS: PROOF EXISTS (run 'ots verify' to check)"
      fi
    else
      echo "  OTS: PROOF EXISTS (install opentimestamps-client to verify)"
    fi
  else
    echo "  OTS: MISSING"
    BREAK=1
  fi

  PREV_HASH=$FILE_HASH
  echo ""
done

if [ "$BREAK" -eq 1 ]; then
  echo "❌ CHAIN BROKEN — some checks failed."
  exit 1
else
  echo "✅ CHAIN INTACT — $COUNT canary files verified."
fi
