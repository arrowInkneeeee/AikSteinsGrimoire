#!/usr/bin/env bash
#
# install.sh - expose the curated aik-skills-lab skills to DeepSeek Harness (DSH)
# by symlinking them into a DSH skill root. The canonical skill files stay in
# aik-skills-lab/ (shared with Lingma/Qoder/Claude), so edits are reflected live
# instead of being copied and going stale.
#
# Usage:
#   ./dsh-migration/install.sh                     # user scope (default): ~/.dsh/skills, all sessions
#   ./dsh-migration/install.sh --set all           # also install the optional skills
#   ./dsh-migration/install.sh --project           # project scope: <repo>/.agents/skills only
#   ./dsh-migration/install.sh --dry-run           # print actions only
#   ./dsh-migration/install.sh --uninstall         # remove links created by this script
#   ./dsh-migration/install.sh --project --uninstall
#   ./dsh-migration/install.sh --verify            # verify an existing install
#
# DSH skill roots (dsh-skill-filesystem, default config):
#   <project>/.dsh/skills        rank 100
#   <project>/.agents/skills     rank 200   <- project install target
#   customSkillDirs              rank 300
#   ~/.dsh/skills                rank 400   <- global install target
#   ~/.agents/skills             rank 500

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
MANIFEST="$SCRIPT_DIR/skills.manifest"
VERIFIER="$SCRIPT_DIR/tools/list-skills.mjs"

SCOPE="global"
SET="core"
MODE="install"
DRY_RUN=0

usage() { sed -n '2,29p' "$0" | sed 's/^# \{0,1\}//'; }

while (($# > 0)); do
  case "$1" in
    --global) SCOPE="global" ;;
    --project) SCOPE="project" ;;
    --set) [[ $# -ge 2 ]] || { echo "error: --set needs core|all" >&2; exit 2; }; SET="$2"; shift ;;
    --uninstall) MODE="uninstall" ;;
    --verify) MODE="verify" ;;
    --dry-run) DRY_RUN=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown option '$1'" >&2; usage; exit 2 ;;
  esac
  shift
done

[[ "$SET" == "core" || "$SET" == "all" ]] || { echo "error: --set must be core or all" >&2; exit 2; }

if [[ "$SCOPE" == "global" ]]; then
  TARGET_ROOT="${DSH_HOME:-$HOME/.dsh}/skills"
else
  TARGET_ROOT="$REPO_ROOT/.agents/skills"
fi

# Rows: link-name <TAB> source <TAB> set <TAB> catalog-name
entries() {
  awk -F'\t' 'NF >= 4 && $1 !~ /^#/ && $1 != "link-name" { print }' "$MANIFEST"
}

selected() {
  # --set all installs every row; core installs only rows whose set is core
  entries | awk -F'\t' -v want="$SET" '$3 == "core" || want == "all"'
}

scope_label="$SCOPE ($TARGET_ROOT)"
installed_count=0
skipped_count=0
removed_count=0

do_install() {
  [[ -d "$REPO_ROOT/aik-skills-lab" ]] || { echo "error: aik-skills-lab/ not found under $REPO_ROOT" >&2; exit 1; }
  if [[ $DRY_RUN -eq 0 && ! -d "$TARGET_ROOT" ]]; then
    mkdir -p "$TARGET_ROOT" || {
      echo "error: cannot create $TARGET_ROOT" >&2
      echo "hint: the DSH profile/home may be read-only in this environment; run this script on the DSH host." >&2
      exit 1
    }
  fi

  echo "scope  : $scope_label"
  echo "set    : $SET"
  echo

  while IFS=$'\t' read -r link_name rel_source set catalog_name; do
    local source="$REPO_ROOT/$rel_source"
    local link="$TARGET_ROOT/$link_name"

    if [[ ! -f "$source/SKILL.md" ]]; then
      echo "skip   $link_name - missing $rel_source/SKILL.md" >&2
      skipped_count=$((skipped_count + 1)); continue
    fi
    if [[ -e "$link" && ! -L "$link" ]]; then
      echo "skip   $link_name - $link exists and is not a symlink" >&2
      skipped_count=$((skipped_count + 1)); continue
    fi

    local target
    if [[ "$SCOPE" == "global" ]]; then
      target="$source"
    else
      target="$(realpath --relative-to="$TARGET_ROOT" "$source")"
    fi

    if [[ $DRY_RUN -eq 1 ]]; then
      echo "would  $link_name -> $target   (catalog name: $catalog_name)"
    else
      ln -sfn "$target" "$link"
      echo "link   $link_name -> $target   (catalog name: $catalog_name)"
    fi
    installed_count=$((installed_count + 1))
  done < <(selected)

  echo
  if [[ $DRY_RUN -eq 1 ]]; then
    echo "dry-run: $installed_count to install, $skipped_count skipped"
    return
  fi
  echo "installed: $installed_count, skipped: $skipped_count"
}

do_uninstall() {
  echo "scope  : $scope_label"
  echo
  while IFS=$'\t' read -r link_name rel_source set catalog_name; do
    local source="$REPO_ROOT/$rel_source"
    local link="$TARGET_ROOT/$link_name"
    if [[ -L "$link" ]] && [[ "$(readlink -f "$link")" == "$(readlink -f "$source")" ]]; then
      if [[ $DRY_RUN -eq 1 ]]; then
        echo "would remove $link_name"
      else
        rm -f "$link"
        echo "removed $link_name"
      fi
      removed_count=$((removed_count + 1))
    fi
  done < <(selected)

  echo
  if [[ -d "$TARGET_ROOT" ]] && [[ -z "$(ls -A "$TARGET_ROOT" 2>/dev/null)" ]]; then
    [[ $DRY_RUN -eq 1 ]] || rmdir "$TARGET_ROOT" 2>/dev/null || true
  fi
  echo "removed: $removed_count"
}

do_verify() {
  echo "scope  : $scope_label"
  echo
  if [[ ! -d "$TARGET_ROOT" ]]; then
    echo "error: $TARGET_ROOT does not exist; run ./dsh-migration/install.sh first" >&2
    exit 1
  fi
  if ! command -v node >/dev/null 2>&1; then
    echo "error: node is required to run the verifier" >&2
    exit 1
  fi
  local expect
  expect="$(selected | cut -f4 | paste -sd, -)"
  node "$VERIFIER" --expect "$expect" "$TARGET_ROOT"
}

case "$MODE" in
  install) do_install ;;
  uninstall) do_uninstall ;;
  verify) do_verify ;;
esac

if [[ "$MODE" == "install" && $DRY_RUN -eq 0 ]]; then
  echo
  echo "next: ./dsh-migration/install.sh --verify"
  echo "      then start a new DSH session, or let the running service refresh its catalog."
fi
