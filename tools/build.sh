#!/usr/bin/env bash
set -euo pipefail
game_dir="$(cd "$(dirname "$0")/.." && pwd)"
engine_dir="${MEGALODONTE_ENGINE:-$game_dir/../../megalodonte-declarative/megalodonte-engine}"
case "${1:-all}" in
 desktop|appimage) bash "$engine_dir/scripts/build-desktop.sh" "$game_dir" ;;
 android-release) bash "$engine_dir/scripts/export-android-release.sh" "$game_dir" ;;
 android) bash "$engine_dir/scripts/build-android.sh" "$game_dir" ;;
 all) bash "$engine_dir/scripts/build-desktop.sh" "$game_dir"; bash "$engine_dir/scripts/build-android.sh" "$game_dir" ;;
 *) echo "Uso: bash tools/build.sh [desktop|appimage|android|android-release|all]" >&2; exit 1 ;;
esac
