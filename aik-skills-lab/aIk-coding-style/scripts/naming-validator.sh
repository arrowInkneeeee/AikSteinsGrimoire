#!/usr/bin/env bash
# =============================================================================
# naming-validator.sh — Java naming convention validator
# =============================================================================
#
# Usage:
#   ./naming-validator.sh [directory]
#
#   Scans all *.java files under the given directory (default: current working
#   directory) and checks the following naming conventions:
#
#     Convention          Regex                                      Example
#     ──────────────────────────────────────────────────────────────────────
#     Class/Interface     ^[A-Z][a-zA-Z0-9]+$                       UserService
#     Enum                ^[A-Z][a-zA-Z0-9]+$                       OrderStatus
#     Annotation          ^[A-Z][a-zA-Z0-9]+$                       Override
#     Method              ^[a-z][a-zA-Z0-9]+$                       getUserById
#     Variable / Field    ^[a-z][a-zA-Z0-9]+$                       userName
#     Constant (static    ^[A-Z][A-Z0-9_]+$                         MAX_RETRY_COUNT
#       final)
#
#   The script also validates that the file's package declaration uses only
#   lowercase letters, digits, and dots (no underscores, no uppercase).
#
# Exit codes:
#   0 — no violations found
#   1 — one or more naming violations detected
#   2 — usage / argument error
#
# Limitations:
#   - Relies on basic regex matching against source lines.  It will NOT catch
#     every edge case (multi-line annotations, nested classes declared in
#     unusual positions, etc.).  For comprehensive enforcement, combine with
#     Checkstyle or SonarLint.
# =============================================================================

set -euo pipefail

# --- helper functions --------------------------------------------------------

usage() {
    sed -n '/^# Usage:/,/^#$/p' "$0" | sed 's/^# //'
}

# ANSI colours (optional — remove if your terminal does not support them).
RED='\033[0;31m'
YEL='\033[0;33m'
NC='\033[0m'  # No Colour

# --- argument parsing --------------------------------------------------------

SCAN_DIR="${1:-.}"

if [[ ! -d "$SCAN_DIR" ]]; then
    echo "ERROR: '$SCAN_DIR' is not a directory or does not exist." >&2
    usage
    exit 2
fi

# --- counters ----------------------------------------------------------------

VIOLATIONS=0
FILES_CHECKED=0

# --- validation patterns -----------------------------------------------------

# Matches a valid PascalCase name (start with uppercase, rest alphanumeric).
readonly PASCAL_RE='^[A-Z][a-zA-Z0-9]+$'

# Matches a valid camelCase name (start with lowercase, rest alphanumeric).
readonly CAMEL_RE='^[a-z][a-zA-Z0-9]+$'

# Matches a valid UPPER_SNAKE_CASE constant name.
readonly CONSTANT_RE='^[A-Z][A-Z0-9_]+$'

# Matches a valid package declaration (only lowercase, digits, dots).
readonly PACKAGE_RE='^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$'

# --- validation functions ----------------------------------------------------

check_class() {
    local name="$1" file="$2" line="$3"
    [[ "$name" =~ $PASCAL_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [class name]   %s  →  '%s' is not PascalCase  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_interface() {
    local name="$1" file="$2" line="$3"
    [[ "$name" =~ $PASCAL_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [interface]    %s  →  '%s' is not PascalCase  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_enum() {
    local name="$1" file="$2" line="$3"
    [[ "$name" =~ $PASCAL_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [enum]         %s  →  '%s' is not PascalCase  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_annotation() {
    local name="$1" file="$2" line="$3"
    [[ "$name" =~ $PASCAL_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [annotation]   %s  →  '%s' is not PascalCase  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_method() {
    local name="$1" file="$2" line="$3"
    # Skip constructors (they match PascalCase by design).
    [[ "$name" =~ $PASCAL_RE ]] && return 0
    [[ "$name" =~ $CAMEL_RE  ]] && return 0
    printf "${RED}VIOLATION${NC} [method]       %s  →  '%s' is not camelCase   (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_field() {
    local name="$1" file="$2" line="$3"
    # Allow UPPER_SNAKE_CASE for static final constants (handled separately).
    # For regular fields require camelCase.
    [[ "$name" =~ $CAMEL_RE    ]] && return 0
    [[ "$name" =~ $CONSTANT_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [field]        %s  →  '%s' naming is invalid  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_constant() {
    local name="$1" file="$2" line="$3"
    [[ "$name" =~ $CONSTANT_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [constant]     %s  →  '%s' is not UPPER_SNAKE_CASE  (%s:%s)\n" "$file" "$name" "$file" "$line"
    ((VIOLATIONS++)) || true
}

check_package() {
    local pkg="$1" file="$2" line="$3"
    [[ "$pkg" =~ $PACKAGE_RE ]] && return 0
    printf "${RED}VIOLATION${NC} [package]      %s  →  '%s' contains invalid characters  (%s:%s)\n" "$file" "$pkg" "$file" "$line"
    ((VIOLATIONS++)) || true
}

# --- file processor ----------------------------------------------------------

process_file() {
    local file="$1"
    ((FILES_CHECKED++))

    # Read the file content once to avoid re-reading.
    local content
    content=$(<"$file")

    # --- package check ---
    if echo "$content" | grep -qP '^\s*package\s+\S+'; then
        local pkg
        pkg=$(echo "$content" | grep -oP '^\s*package\s+\K[\w.]+' | head -1)
        local pkg_line
        pkg_line=$(echo "$content" | grep -nP '^\s*package\s+' | head -1 | cut -d: -f1)
        check_package "$pkg" "$file" "${pkg_line:-1}"
    fi

    # --- class / interface / enum detection ---
    # Match lines like:  public class Foo {   or   public interface Bar extends Baz {
    while IFS= read -r match_line; do
        local lnr
        lnr=$(echo "$match_line" | cut -d: -f1)
        local line
        line=$(echo "$match_line" | cut -d: -f2-)

        # Extract the name — the word immediately after 'class', 'interface',
        # 'enum', or '@interface'.
        local name=""

        # @interface (annotation)
        if echo "$line" | grep -qP '\b@interface\s+[A-Za-z]'; then
            name=$(echo "$line" | sed -n 's/.*@interface\s\+\([A-Za-z][A-Za-z0-9_]*\).*/\1/p')
            check_annotation "$name" "$file" "$lnr"

        # class
        elif echo "$line" | grep -qP '\bclass\s+[A-Za-z]'; then
            name=$(echo "$line" | sed -n 's/.*\<class\>\s\+\([A-Za-z][A-Za-z0-9_]*\).*/\1/p')
            check_class "$name" "$file" "$lnr"

        # interface (but not @interface)
        elif echo "$line" | grep -qP '(?<!@)\binterface\s+[A-Za-z]'; then
            name=$(echo "$line" | sed -n 's/.*\binterface\s\+\([A-Za-z][A-Za-z0-9_]*\).*/\1/p')
            check_interface "$name" "$file" "$lnr"

        # enum
        elif echo "$line" | grep -qP '\benum\s+[A-Za-z]'; then
            name=$(echo "$line" | sed -n 's/.*\benum\s\+\([A-Za-z][A-Za-z0-9_]*\).*/\1/p')
            check_enum "$name" "$file" "$lnr"
        fi
    done < <(echo "$content" | grep -nP '(^\s*(public|private|protected|static|abstract|final)*\s*(class|interface|enum|@interface)\s+[A-Za-z])')

    # --- method detection ---
    # Look for lines that match a typical method signature:
    #   [modifiers] returnType methodName (params) [throws ...] {
    # We avoid constructors by ignoring names that match PascalCase.
    while IFS= read -r match_line; do
        local lnr
        lnr=$(echo "$match_line" | cut -d: -f1)
        local line
        line=$(echo "$match_line" | cut -d: -f2-)

        local name
        name=$(echo "$line" | sed -n 's/.*\s\+\([a-zA-Z_][a-zA-Z0-9_]*\)\s*(\s*[^)]*\s*).*/\1/p')
        if [[ -n "$name" ]]; then
            check_method "$name" "$file" "$lnr"
        fi
    done < <(echo "$content" | grep -nP '^\s+(public|private|protected)?\s*(static|final|synchronized|native|abstract)*\s*[A-Za-z][\w<>\[\],\s]+\s+[a-zA-Z_]\w*\s*\([^)]*\)\s*(throws\s+\S+)?\s*\{' || true)

    # --- field detection ---
    # Lines containing a typical field declaration (type + name + semicolon or =).
    # We split between constants (static final) and regular fields.
    while IFS= read -r match_line; do
        local lnr
        lnr=$(echo "$match_line" | cut -d: -f1)
        local line
        line=$(echo "$match_line" | cut -d: -f2-)

        local name
        name=$(echo "$line" | sed -n 's/.*\s\+\([a-zA-Z_][a-zA-Z0-9_]*\)\s*[=;].*/\1/p')

        if [[ -z "$name" ]]; then
            continue
        fi

        # Skip type names, keywords, and obvious non-fields.
        case "$name" in
            int|long|short|byte|char|boolean|double|float|void|
            String|Integer|Long|Short|Byte|Char|Boolean|Double|Float|Void|
            public|private|protected|static|final|class|new|return|if|for|while|throw|throws)
                continue
                ;;
        esac

        # Distinguish constants from regular fields.
        if echo "$line" | grep -qP '(static\s+final|final\s+static)'; then
            check_constant "$name" "$file" "$lnr"
        else
            check_field "$name" "$file" "$lnr"
        fi
    done < <(echo "$content" | grep -nP '^\s+(private|protected|public)\s+(static\s+final|final\s+static|static|final)?\s*[A-Za-z][\w<>\[\]]+\s+[a-zA-Z_]\w*\s*[=;]' || true)
}

# --- main loop ---------------------------------------------------------------

echo "Scanning *.java files under: $SCAN_DIR"
echo ""

while IFS= read -r -d '' java_file; do
    process_file "$java_file"
done < <(find "$SCAN_DIR" -type f -name '*.java' -print0)

# --- summary -----------------------------------------------------------------

echo ""
echo "====================================="
echo " Naming Convention Check Summary"
echo "====================================="
printf " Directory scanned  : %s\n" "$SCAN_DIR"
printf " Files checked      : %d\n" "$FILES_CHECKED"
printf " Violations found   : %d\n" "$VIOLATIONS"
echo "====================================="

if (( VIOLATIONS > 0 )); then
    exit 1
fi

exit 0
