#!/usr/bin/env bash
set -euo pipefail

# Generates a categorized starting draft changelog between two git references.
# Note: Edit the draft to keep release notes concise (1–2 sentences) and focused
# on observable runtime and bot behaviour changes rather than internal mechanics.
# Usage: ./generate-changelog.sh [<from-tag>] [<to-tag>]

FROM_REF="${1:-}"
TO_REF="${2:-HEAD}"

if [[ -z "${FROM_REF}" ]]; then
  # Find previous tag from TO_REF
  FROM_REF=$(git describe --tags --abbrev=0 "${TO_REF}^" 2>/dev/null || true)
  if [[ -z "${FROM_REF}" ]]; then
    echo "Error: Could not automatically detect a previous tag. Please specify FROM_REF." >&2
    echo "Usage: $0 <from-ref> [to-ref]" >&2
    exit 1
  fi
fi

# Fetch commit subjects, ignoring merge and release-prep commits
COMMITS=$(git log "${FROM_REF}..${TO_REF}" --no-merges --pretty=format:"%s" | grep -v -E "^Prepare for (development|release)" || true)

FEATURES=()
ENHANCEMENTS=()
FIXES=()
TECHNICAL=()

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  if [[ "$line" =~ ^(Add|Support|Create) ]]; then
    FEATURES+=("• $line")
  elif [[ "$line" =~ ^(Fix|Resolve|Prevent|Correct) ]]; then
    FIXES+=("• $line")
  elif [[ "$line" =~ ^(Update|Change|Match|Catch|Cache|Improve|Optimize|Refactor) ]]; then
    ENHANCEMENTS+=("• $line")
  else
    TECHNICAL+=("• $line")
  fi
done <<< "$COMMITS"

OUTPUT=""

if [[ ${#FEATURES[@]} -gt 0 ]]; then
  OUTPUT+=$'## ✨ New Features\n\n'
  for item in "${FEATURES[@]}"; do
    OUTPUT+="${item}"$'\n'
  done
  OUTPUT+=$'\n'
fi

if [[ ${#ENHANCEMENTS[@]} -gt 0 ]]; then
  OUTPUT+=$'## ⚙️ Enhancements\n\n'
  for item in "${ENHANCEMENTS[@]}"; do
    OUTPUT+="${item}"$'\n'
  done
  OUTPUT+=$'\n'
fi

if [[ ${#FIXES[@]} -gt 0 ]]; then
  OUTPUT+=$'## 🐛 Bug Fixes\n\n'
  for item in "${FIXES[@]}"; do
    OUTPUT+="${item}"$'\n'
  done
  OUTPUT+=$'\n'
fi

if [[ ${#TECHNICAL[@]} -gt 0 ]]; then
  OUTPUT+=$'## 🔧 Technical\n\n'
  for item in "${TECHNICAL[@]}"; do
    OUTPUT+="${item}"$'\n'
  done
  OUTPUT+=$'\n'
fi

REPO_URL=$(git remote get-url origin 2>/dev/null | sed -E 's#(git@|https://)([^:/]+)[:/]([^/]+)/([^/.]+)(\.git)?#https://\2/\3/\4#' || true)
if [[ -z "${REPO_URL}" ]]; then
  REPO_URL="https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || echo 'MrBean355/zak-bagans-bot')"
fi

OUTPUT+="Full Changelog: ${REPO_URL}/compare/${FROM_REF}...${TO_REF}"$'\n'

echo "$OUTPUT"
