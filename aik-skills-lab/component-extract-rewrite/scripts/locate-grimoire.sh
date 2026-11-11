#!/usr/bin/env bash
#
# locate-grimoire.sh — 只读定位"本机魔典"（AikSteinsGrimoire）目录。
#
# 设计依据：grimoire-files/plans/knowledge-ingestion-rework.md §4
#   - 先检测环境（WSL2 / Windows 原生 / 独立 VM / 容器），再按环境选路
#   - 只读；默认不写任何文件，不写环境变量，不写 pending 目录
#   - anchor 只是"书签"（~/.aik/grimoire-anchor.json），需显式 --remember 才写
#   - 失败输出 NEED_PATH 并以非 0 退出，绝不猜测后继续
#
# 用法：
#   bash locate-grimoire.sh                 # 人类可读输出
#   bash locate-grimoire.sh --json          # JSON 输出（供技能/脚本消费）
#   bash locate-grimoire.sh --no-scan       # 跳过有界扫描（只走 cwd/anchor/env）
#   bash locate-grimoire.sh --root <dir>    # 追加一个扫描根（可重复）
#   bash locate-grimoire.sh --remember      # 成功后把 anchor 写入 ~/.aik/（交互确认）
#   bash locate-grimoire.sh --remember --yes  # 非交互写 anchor（调用方必须先取得用户批准）
#
# 退出码：0 = 命中；2 = NEED_PATH（未找到，未写入）；1 = 参数/环境错误
#
# 注意：本脚本不重命名任何 aIk 标识；AIK_GRIMOIRE_HOME 只读。

set -u

SCRIPT_NAME="$(basename -- "$0")"
JSON=0
REMEMBER=0
ASSUME_YES=0
SCAN=1
EXTRA_ROOTS=()
ANCHOR_DIR="${HOME:-}/.aik"
ANCHOR_FILE="$ANCHOR_DIR/grimoire-anchor.json"

usage() {
  sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
}

while (($# > 0)); do
  case "$1" in
    --json) JSON=1 ;;
    --remember) REMEMBER=1 ;;
    --yes) ASSUME_YES=1 ;;
    --no-scan) SCAN=0 ;;
    --root)
      [[ $# -ge 2 ]] || { echo "error: --root 需要一个目录参数" >&2; exit 1; }
      EXTRA_ROOTS+=("$2"); shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: 未知参数 '$1'" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

# ---------------------------------------------------------------- helpers ----

json_escape() {
  # 字符串 → JSON 字符串内容（不包引号）
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/\t/\\t/g' -e 's/\r//g' \
    -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\n/g'
}

is_grimoire_strong() {
  # M1-M5；扫描候选必须全部通过
  local p="$1"
  [[ -d "$p" ]] || return 1
  [[ -f "$p/grimoire-files/README.md" ]] || return 1
  [[ -f "$p/grimoire-scrolls/README.md" ]] || return 1
  [[ -d "$p/src/main/java/io/aik/steins/grimoire/knowledge" ]] || return 1
  [[ -f "$p/pom.xml" ]] || return 1
  grep -qE 'AikSteinsGrimoire|aik-steins-grimoire' "$p/pom.xml" 2>/dev/null || return 1
  if command -v git >/dev/null 2>&1; then
    local remote
    remote="$(git -C "$p" remote get-url origin 2>/dev/null || true)"
    [[ -n "$remote" ]] || return 1
    printf '%s' "$remote" | grep -qi 'AikSteinsGrimoire' || return 1
  fi
  return 0
}

is_grimoire_markers() {
  # M1-M4（anchor/env 显式指向时接受；git remote 只作加分项）
  local p="$1"
  [[ -d "$p" ]] || return 1
  [[ -f "$p/grimoire-files/README.md" ]] || return 1
  [[ -f "$p/grimoire-scrolls/README.md" ]] || return 1
  [[ -d "$p/src/main/java/io/aik/steins/grimoire/knowledge" ]] || return 1
  [[ -f "$p/pom.xml" ]] || return 1
  grep -qE 'AikSteinsGrimoire|aik-steins-grimoire' "$p/pom.xml" 2>/dev/null || return 1
  return 0
}

remote_of() { git -C "$1" remote get-url origin 2>/dev/null | head -n1 || true; }
head_of()   { git -C "$1" rev-parse --short HEAD 2>/dev/null | head -n1 || true; }

detect_env() {
  # stdout: wsl2 | windows | vm-shared | vm-isolated | container | linux
  if [[ -e /proc/version ]] && grep -qi 'microsoft' /proc/version 2>/dev/null; then
    echo "wsl2"; return
  fi
  if [[ -n "${WSL_DISTRO_NAME:-}" ]] || command -v wslpath >/dev/null 2>&1 && [[ -d /mnt ]]; then
    echo "wsl2"; return
  fi
  if [[ -e /.dockerenv ]] || grep -qE 'docker|containerd' /proc/1/cgroup 2>/dev/null; then
    echo "container"; return
  fi
  if command -v systemd-detect-virt >/dev/null 2>&1; then
    local virt
    virt="$(systemd-detect-virt 2>/dev/null || true)"
    case "$virt" in
      vmware|virtualbox|kvm|qemu|microsoft) ;; # 继续看共享目录
      none) echo "linux"; return ;;
    esac
  fi
  if [[ -d /mnt/hgfs ]] || compgen -G "/media/sf_*" >/dev/null 2>&1; then
    echo "vm-shared"; return
  fi
  local uname_s
  uname_s="$(uname -s 2>/dev/null || echo unknown)"
  case "$uname_s" in
    MINGW*|MSYS*|CYGWIN*) echo "windows"; return ;;
  esac
  if command -v systemd-detect-virt >/dev/null 2>&1; then
    local v; v="$(systemd-detect-virt 2>/dev/null || true)"
    [[ -n "$v" && "$v" != "none" ]] && { echo "vm-isolated"; return; }
  fi
  echo "linux"
}

# 扫描根（只读、有界）
scan_roots() {
  local env="$1"
  local -a roots=()
  local r
  for r in ${AIK_GRIMOIRE_SCAN_ROOTS:-}; do roots+=("$r"); done
  if [[ "$env" == "wsl2" ]]; then
    roots+=("/mnt/d/JeBrainsWorkSpace" "${HOME:-}/Workspace" "${HOME:-}/workspace" "${HOME:-}")
  elif [[ "$env" == "windows" ]]; then
    roots+=("D:/JeBrainsWorkSpace" "${USERPROFILE:-}/Workspace" "${USERPROFILE:-}")
  elif [[ "$env" == "vm-shared" ]]; then
    roots+=("/mnt/hgfs" "/media/sf_*" "${HOME:-}")
  else
    roots+=("${HOME:-}" "${HOME:-}/Workspace")
  fi
  for r in "${EXTRA_ROOTS[@]:-}"; do [[ -n "$r" ]] && roots+=("$r"); done
  printf '%s\n' "${roots[@]}" | awk 'NF' | sort -u
}

# 有界扫描：在 root 下 maxdepth 3 找名为 AikSteinsGrimoire 的目录
scan_for_grimoire() {
  local env="$1"
  local -a hits=()
  local root
  while IFS= read -r root; do
    [[ -n "$root" ]] || continue
    # 处理 /media/sf_* 之类的通配
    if [[ "$root" == *'*'* ]]; then
      # shellcheck disable=SC2086
      set -- $root
      [[ -e "$1" ]] || continue
      root="$1"
    fi
    [[ -d "$root" ]] || continue
    local found=""
    if command -v timeout >/dev/null 2>&1; then
      found="$(timeout 10 find "$root" -maxdepth 3 -type d -name 'AikSteinsGrimoire' 2>/dev/null || true)"
    else
      found="$(find "$root" -maxdepth 3 -type d -name 'AikSteinsGrimoire' 2>/dev/null || true)"
    fi
    local p
    while IFS= read -r p; do
      [[ -n "$p" ]] || continue
      if is_grimoire_strong "$p"; then hits+=("$p"); fi
    done <<< "$found"
  done < <(scan_roots "$env")
  printf '%s\n' "${hits[@]:-}" | awk 'NF' | sort -u
}

# 从 anchor JSON 中提取候选路径（wsl / windows / path 三种键）
anchor_paths() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  # 提取 "wsl": "..."、 "windows": "..."、 "path": "..."
  sed -n 's/.*"\(wsl\|windows\|path\)"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\2/p' "$file" 2>/dev/null \
    | while IFS= read -r p; do
        # 还原 JSON 转义：\\ → \  ，\" → "
        printf '%s\n' "$p" | sed -e 's/\\\\/\\/g' -e 's/\\"/"/g'
      done
}

anchor_candidates() {
  local -a files=()
  [[ -n "${HOME:-}" ]] && files+=("$HOME/.aik/grimoire-anchor.json")
  [[ -n "${DSH_HOME:-}" ]] && files+=("$DSH_HOME/aik/grimoire-anchor.json")
  # WSL 下也看 Windows 用户目录里的 anchor（跨宿主书签）
  if [[ "$(detect_env)" == "wsl2" ]]; then
    local w
    for w in /mnt/c/Users/*/.aik/grimoire-anchor.json; do
      [[ -f "$w" ]] && files+=("$w")
    done
  fi
  local f
  for f in "${files[@]:-}"; do
    [[ -n "$f" ]] && anchor_paths "$f"
  done | awk 'NF' | sort -u
}

translate_cross_host() {
  # wslpath 可用于 WSL ↔ Windows 路径互转；其他环境原样输出
  local p="$1" env="$2"
  if [[ "$env" == "wsl2" ]] && command -v wslpath >/dev/null 2>&1; then
    if [[ "$p" == /mnt/* ]]; then wslpath -w "$p" 2>/dev/null || true; fi
    if [[ "$p" == [A-Za-z]:\\* || "$p" == [A-Za-z]:/* ]]; then wslpath -u "$p" 2>/dev/null || true; fi
  fi
}

# ---------------------------------------------------------------- locate ----
ENV_TYPE="$(detect_env)"
SOURCE=""
HIT=""
ATTEMPTED=()

find_from_cwd() {
  local d="$PWD"
  local i=0
  while [[ -n "$d" && "$d" != "/" && $i -lt 12 ]]; do
    ATTEMPTED+=("cwd:$d")
    if is_grimoire_markers "$d"; then HIT="$d"; SOURCE="cwd"; return 0; fi
    d="$(dirname "$d")"
    i=$((i + 1))
  done
  return 1
}

find_from_anchor() {
  local p
  while IFS= read -r p; do
    [[ -n "$p" ]] || continue
    ATTEMPTED+=("anchor:$p")
    if is_grimoire_markers "$p"; then HIT="$p"; SOURCE="anchor"; return 0; fi
    local t; t="$(translate_cross_host "$p" "$ENV_TYPE")"
    [[ -n "$t" ]] && ATTEMPTED+=("anchor-t:$t")
    if [[ -n "$t" ]] && is_grimoire_markers "$t"; then HIT="$t"; SOURCE="anchor"; return 0; fi
  done < <(anchor_candidates)
  return 1
}

find_from_env() {
  local p="${AIK_GRIMOIRE_HOME:-}"
  [[ -n "$p" ]] || return 1
  ATTEMPTED+=("env:$p")
  if is_grimoire_markers "$p"; then HIT="$p"; SOURCE="env"; return 0; fi
  local t; t="$(translate_cross_host "$p" "$ENV_TYPE")"
  [[ -n "$t" ]] && ATTEMPTED+=("env-t:$t")
  if [[ -n "$t" ]] && is_grimoire_markers "$t"; then HIT="$t"; SOURCE="env"; return 0; fi
  return 1
}

find_from_scan() {
  [[ "$SCAN" -eq 1 ]] || return 1
  local -a hits=()
  local p
  while IFS= read -r p; do
    [[ -n "$p" ]] || continue
    hits+=("$p")
    ATTEMPTED+=("scan:$p")
  done < <(scan_for_grimoire "$ENV_TYPE")
  if ((${#hits[@]} == 1)); then HIT="${hits[0]}"; SOURCE="scan"; return 0; fi
  if ((${#hits[@]} > 1)); then
    HIT=""
    SOURCE="scan-ambiguous"
    SCAN_HITS="$(printf '%s\n' "${hits[@]}")"
    return 2
  fi
  return 1
}

find_from_cwd || find_from_anchor || find_from_env || find_from_scan || true

# ---------------------------------------------------------------- output ----
SCAN_HITS=""
STATUS="OK"
[[ -n "$HIT" ]] || STATUS="NEED_PATH"
[[ -n "$SCAN_HITS" ]] && STATUS="AMBIGUOUS"

if [[ "$STATUS" == "OK" ]]; then
  HIT="$(cd "$HIT" && pwd -P)"
  REMOTE="$(remote_of "$HIT")"
  HEAD="$(head_of "$HIT")"
  WSL_PATH="$HIT"
  WIN_PATH=""
  if [[ "$ENV_TYPE" == "wsl2" ]] && command -v wslpath >/dev/null 2>&1; then
    WIN_PATH="$(wslpath -w "$HIT" 2>/dev/null || true)"
  elif [[ "$ENV_TYPE" == "windows" ]]; then
    WIN_PATH="$HIT"
  fi
fi

attempted_json() {
  local p out=""
  for p in "${ATTEMPTED[@]:-}"; do
    [[ -n "$p" ]] || continue
    out+="${out:+,}\"$(json_escape "$p")\""
  done
  printf '[%s]' "$out"
}

if [[ "$JSON" -eq 1 ]]; then
  if [[ "$STATUS" == "AMBIGUOUS" ]]; then
    CANDIDATES=""
    while IFS= read -r c; do
      [[ -n "$c" ]] || continue
      CANDIDATES+="${CANDIDATES:+,}\"$(json_escape "$c")\""
    done <<< "$SCAN_HITS"
    cat <<EOF
{
  "status": "AMBIGUOUS",
  "envType": "$(json_escape "$ENV_TYPE")",
  "strategy": "direct",
  "candidates": [${CANDIDATES}],
  "attempted": $(attempted_json)
}
EOF
  elif [[ "$STATUS" == "OK" ]]; then
    cat <<EOF
{
  "status": "OK",
  "envType": "$(json_escape "$ENV_TYPE")",
  "strategy": "direct",
  "path": "$(json_escape "$HIT")",
  "source": "$(json_escape "$SOURCE")",
  "remote": "$(json_escape "${REMOTE:-}")",
  "head": "$(json_escape "${HEAD:-}")",
  "paths": { "wsl": "$(json_escape "${WSL_PATH:-}")", "windows": "$(json_escape "${WIN_PATH:-}")" },
  "attempted": $(attempted_json)
}
EOF
  else
    cat <<EOF
{
  "status": "NEED_PATH",
  "envType": "$(json_escape "$ENV_TYPE")",
  "strategy": "$([[ "$ENV_TYPE" == vm-isolated ]] && echo copy-bridge || echo direct)",
  "attempted": $(attempted_json),
  "hints": [
    "确认魔典目录存在：grimoire-files/README.md、grimoire-scrolls/README.md、src/.../knowledge/、pom.xml 含 AikSteinsGrimoire",
    "可显式传 --root <目录>，或 export AIK_GRIMOIRE_HOME=<路径>（只读）",
    "确认后可用 --remember 把书签写入 ~/.aik/grimoire-anchor.json（需用户批准）"
  ]
}
EOF
  fi
else
  if [[ "$STATUS" == "OK" ]]; then
    echo "status   : OK"
    echo "env      : $ENV_TYPE"
    echo "strategy : direct"
    echo "source   : $SOURCE"
    echo "path     : $HIT"
    echo "remote   : ${REMOTE:-<none>}"
    echo "head     : ${HEAD:-<none>}"
    [[ -n "${WIN_PATH:-}" && "$WIN_PATH" != "$HIT" ]] && echo "windows  : $WIN_PATH"
  elif [[ "$STATUS" == "AMBIGUOUS" ]]; then
    echo "status   : AMBIGUOUS（找到多个候选，必须让用户选）"
    echo "env      : $ENV_TYPE"
    echo "candidates:"
    while IFS= read -r c; do [[ -n "$c" ]] && echo "  - $c"; done <<< "$SCAN_HITS"
  else
    echo "status   : NEED_PATH"
    echo "env      : $ENV_TYPE"
    echo "strategy : $([[ "$ENV_TYPE" == vm-isolated ]] && echo copy-bridge || echo direct)"
    echo "tried    :"
    for local_p in "${ATTEMPTED[@]:-}"; do [[ -n "$local_p" ]] && echo "  - $local_p"; done
    echo "hint     : 请提供魔典目录（--root <dir>）或只读设置 AIK_GRIMOIRE_HOME"
  fi
fi

# ---------------------------------------------------------------- remember --
if [[ "$STATUS" == "OK" && "$REMEMBER" -eq 1 ]]; then
  if [[ "$ASSUME_YES" -ne 1 ]]; then
    echo >&2
    echo "将写入书签：$ANCHOR_FILE" >&2
    echo "内容只包含仓库路径、remote、HEAD，不含凭据。" >&2
    printf '确认写入？[y/N] ' >&2
    read -r answer || true
    case "${answer:-}" in
      y|Y|yes|YES) ;;
      *) echo "已取消（未写入）。" >&2; exit 0 ;;
    esac
  fi
  mkdir -p "$ANCHOR_DIR" 2>/dev/null || { echo "error: 无法创建 $ANCHOR_DIR" >&2; exit 1; }
  chmod 700 "$ANCHOR_DIR" 2>/dev/null || true
  {
    echo '{'
    echo '  "repo": "AikSteinsGrimoire",'
    echo "  \"remote\": \"$(json_escape "${REMOTE:-}")\","
    echo '  "paths": {'
    echo "    \"wsl\": \"$(json_escape "${WSL_PATH:-}")\","
    echo "    \"windows\": \"$(json_escape "${WIN_PATH:-}")\""
    echo '  },'
    echo "  \"fingerprint\": { \"pomName\": \"AikSteinsGrimoire\", \"head\": \"$(json_escape "${HEAD:-}")\" },"
    echo "  \"writtenAt\": \"$(date -Iseconds 2>/dev/null || date)\","
    echo '  "writtenBy": "AI-assisted (user approved)"'
    echo '}'
  } > "$ANCHOR_FILE.tmp" && mv "$ANCHOR_FILE.tmp" "$ANCHOR_FILE"
  chmod 600 "$ANCHOR_FILE" 2>/dev/null || true
  echo "anchor   : 已写入 $ANCHOR_FILE" >&2
fi

case "$STATUS" in OK) exit 0 ;; AMBIGUOUS) exit 3 ;; *) exit 2 ;; esac
