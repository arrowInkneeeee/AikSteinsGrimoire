#!/usr/bin/env bash
# =============================================================================
# bom-checker.sh — Byte Order Mark (BOM) checker for Java files
# =============================================================================
#
# Usage:
#   ./bom-checker.sh [directory]
#
#   If no directory is given, scans the current working directory recursively.
#   The script searches all *.java files for a UTF-8 BOM (U+FEFF / 0xEF 0xBB 0xBF)
#   at the very start of the file.  Files containing a BOM are reported as
#   warnings because many Java tools (javac, Checkstyle, etc.) either reject or
#   mis-handle BOM characters.
#
# Exit codes:
#   0 — no BOM found
#   1 — one or more files with BOM detected
#   2 — usage / argument error
#
# Examples:
#   ./bom-checker.sh                          # scan current directory
#   ./bom-checker.sh src/main/java            # scan a specific directory
#   find . -name '*.java' | xargs -I{} sh -c 'head -c3 "{}" | od -A x -t x1z'
# =============================================================================

set -euo pipefail

# --- helper functions --------------------------------------------------------

usage() {
    sed -n '/^# Usage:/,/^#$/p' "$0" | sed 's/^# //'
}

# --- argument parsing --------------------------------------------------------

SCAN_DIR="${1:-.}"

if [[ ! -d "$SCAN_DIR" ]]; then
    echo "ERROR: '$SCAN_DIR' is not a directory or does not exist." >&2
    usage
    exit 2
fi

# --- main -------------------------------------------------------------------

FILES_WITH_BOM=0

# Use find to locate every .java file, then check the first three bytes.
# The UTF-8 BOM is the byte sequence 0xEF 0xBB 0xBF.
while IFS= read -r -d '' java_file; do
    # Read the first three bytes (or less if the file is shorter).
    first_bytes=$(od -A n -t x1 -N 3 "$java_file" 2>/dev/null || true)

    # Normalise whitespace for reliable comparison.
    read -ra byte_arr <<< "$first_bytes"

    if [[ "${byte_arr[0]}" == "ef" && "${byte_arr[1]}" == "bb" && "${byte_arr[2]}" == "bf" ]]; then
        echo "WARNING: BOM detected — $java_file"
        ((FILES_WITH_BOM++)) || true
    fi
done < <(find "$SCAN_DIR" -type f -name '*.java' -print0)

# --- summary -----------------------------------------------------------------

echo ""
echo "====================================="
echo " BOM Check Summary"
echo "====================================="
printf " Scanned directory : %s\n" "$SCAN_DIR"
printf " Files with BOM     : %d\n" "$FILES_WITH_BOM"
echo "====================================="

if (( FILES_WITH_BOM > 0 )); then
    echo ""
    echo "TIP: Remove the BOM with one of these commands:"
    echo "  sed -i '1s/^\xEF\xBB\xBF//' <file>       (Linux / macOS)"
    echo "  tail -c +4 <file> > tmp && mv tmp <file>   (any POSIX)"
    exit 1
fi

exit 0
