#!/usr/bin/env bash
set -euo pipefail

# 说明：
# - 用途：把本地仓库的 main 分支推送到指定 GitHub 仓库的任意目标分支
# - 特点：为“推送容易中断/异常终止”的场景做了加固（超时/缓冲/压缩/重试/日志）
# - 兼容：macOS / Linux（需要 git）

usage() {
  cat << 'EOF'
用法：
  ./scripts/git-migrate-to-github.sh \
    --repo-url https://github.com/pbeenigg/h_search-engine.git \
    --target-branch <目标分支名> \
    [--remote-name github] \
    [--source-branch main] \
    [--transport auto|ssh|https] \
    [--force] \
    [--retries 5]

示例：
  # 把本地 main 推送到远端的 dev 分支
  ./scripts/git-migrate-to-github.sh --repo-url https://github.com/pbeenigg/h_search-engine.git --target-branch dev

  # 推送到远端新分支 feature/migrate
  ./scripts/git-migrate-to-github.sh --repo-url https://github.com/pbeenigg/h_search-engine.git --target-branch feature/migrate

  # 必要时强制覆盖远端目标分支（谨慎）
  ./scripts/git-migrate-to-github.sh --repo-url https://github.com/pbeenigg/h_search-engine.git --target-branch dev --force

参数说明：
  --repo-url        目标 GitHub 仓库地址（HTTPS 或 SSH 均可）
  --target-branch   目标分支名（远端分支）
  --remote-name     远端名称（默认 github）
  --source-branch   源分支名（默认 main）
  --transport       传输方式：auto(默认，优先 SSH)、ssh、https
  --force           使用 --force-with-lease 推送（比 --force 更安全，但仍需谨慎）
  --retries         推送重试次数（默认 5）
EOF
}

REMOTE_NAME="github"
REPO_URL=""
SOURCE_BRANCH="h"
TARGET_BRANCH=""
TRANSPORT="auto"
FORCE_PUSH="false"
RETRIES=5

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-url)
      REPO_URL="${2:-}"; shift 2;;
    --target-branch)
      TARGET_BRANCH="${2:-}"; shift 2;;
    --remote-name)
      REMOTE_NAME="${2:-}"; shift 2;;
    --source-branch)
      SOURCE_BRANCH="${2:-}"; shift 2;;
    --transport)
      TRANSPORT="${2:-}"; shift 2;;
    --force)
      FORCE_PUSH="true"; shift 1;;
    --retries)
      RETRIES="${2:-}"; shift 2;;
    -h|--help)
      usage; exit 0;;
    *)
      echo "未知参数：$1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ -z "$REPO_URL" || -z "$TARGET_BRANCH" ]]; then
  echo "参数缺失：--repo-url 和 --target-branch 是必填项" >&2
  usage
  exit 2
fi

if ! command -v git >/dev/null 2>&1; then
  echo "未找到 git，请先安装 git" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "当前目录不是 git 仓库根目录或其子目录，请在项目目录中执行" >&2
  exit 1
fi

is_github_https_url() {
  [[ "$1" =~ ^https://github\.com/[^/]+/[^/]+(\.git)?/?$ ]]
}

github_https_to_ssh_url() {
  local url="$1"
  url="${url%/}"
  url="${url%.git}"
  local path="${url#https://github.com/}"
  echo "git@github.com:${path}.git"
}

is_ssh_github_available() {
  local out
  out="$(ssh -o BatchMode=yes -o ConnectTimeout=8 -T git@github.com 2>&1 || true)"
  echo "$out" | grep -qi "successfully authenticated"
}

# 日志目录
mkdir -p ./logs
TS="$(date +"%Y%m%d_%H%M%S")"
LOG_FILE="./logs/git_migrate_to_github_${TS}.log"

echo "[信息] 日志输出：${LOG_FILE}"

# 将 stdout/stderr 同时输出到控制台与日志
exec > >(tee -a "${LOG_FILE}") 2>&1

echo "[信息] 源分支：${SOURCE_BRANCH}"
echo "[信息] 目标远端：${REMOTE_NAME} => ${REPO_URL}"
echo "[信息] 目标分支：${TARGET_BRANCH}"

if [[ "${TRANSPORT}" != "auto" && "${TRANSPORT}" != "ssh" && "${TRANSPORT}" != "https" ]]; then
  echo "[错误] --transport 参数非法：${TRANSPORT}（仅支持 auto|ssh|https）" >&2
  exit 2
fi

if [[ "${TRANSPORT}" == "auto" ]]; then
  if is_ssh_github_available; then
    TRANSPORT="ssh"
    echo "[信息] 检测到 SSH 可用，自动选择 SSH 方式推送"
  else
    TRANSPORT="https"
    echo "[信息] 未检测到 SSH 可用，自动选择 HTTPS 方式推送"
  fi
fi

if [[ "${TRANSPORT}" == "ssh" ]]; then
  if is_github_https_url "${REPO_URL}"; then
    echo "[信息] 已选择 SSH 方式，自动将 HTTPS URL 转换为 SSH URL"
    REPO_URL="$(github_https_to_ssh_url "${REPO_URL}")"
  fi
fi

# 确保源分支存在
if ! git show-ref --verify --quiet "refs/heads/${SOURCE_BRANCH}"; then
  echo "[错误] 本地分支不存在：${SOURCE_BRANCH}" >&2
  echo "[提示] 你可以先执行：git branch -a 查看分支" >&2
  exit 1
fi

# 切换到源分支（避免 detached HEAD）
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD || true)"
if [[ "${CURRENT_BRANCH}" != "${SOURCE_BRANCH}" ]]; then
  echo "[信息] 切换到源分支：${SOURCE_BRANCH}"
  git checkout "${SOURCE_BRANCH}"
fi

# 配置远端（不存在则新增，存在则更新 URL）
if git remote get-url "${REMOTE_NAME}" >/dev/null 2>&1; then
  EXISTING_URL="$(git remote get-url "${REMOTE_NAME}")"
  if [[ "${EXISTING_URL}" != "${REPO_URL}" ]]; then
    echo "[信息] 更新远端 ${REMOTE_NAME} URL：${EXISTING_URL} -> ${REPO_URL}"
    git remote set-url "${REMOTE_NAME}" "${REPO_URL}"
  else
    echo "[信息] 远端 ${REMOTE_NAME} 已存在且 URL 一致"
  fi
else
  echo "[信息] 新增远端：${REMOTE_NAME} => ${REPO_URL}"
  git remote add "${REMOTE_NAME}" "${REPO_URL}"
fi

# 为“推送中途异常终止”做稳健性配置（仅影响当前仓库）
# 说明：
# - http.postBuffer：增大 HTTP 推送缓冲（仅 HTTPS 有意义）
# - core.compression：降低压缩（CPU 压力小，包更大；网络差时可自行改回默认）
# - http.lowSpeedLimit/Time：避免低速时过早断开（仅 HTTPS 有意义）
# - pack.*：减少打包时的内存峰值
# 这些配置并不一定对所有环境都最优，但对“容易中断”的场景通常更稳。
echo "[信息] 写入 git 推送稳健性配置（本仓库级别）"
if [[ "${TRANSPORT}" == "https" ]]; then
  git config --local http.postBuffer 524288000 || true
  git config --local http.lowSpeedLimit 0 || true
  git config --local http.lowSpeedTime 999999 || true
fi
git config --local core.compression 0 || true

git config --local pack.windowMemory 100m || true
git config --local pack.packSizeLimit 100m || true
git config --local pack.threads 1 || true

# 显示关键信息，方便排查
echo "[信息] Git 版本：$(git --version)"
echo "[信息] 远端列表："
git remote -v

# 可选：简单提醒大文件（GitHub 单文件 100MB 限制）
# 这里不做全量扫描（可能很慢），只提醒你如果仍失败，优先排查大文件或 LFS。
echo "[提示] 如果仍推不上去，优先排查："
echo "[提示] 1) 是否存在 >100MB 的大文件（GitHub 会拒绝）"
echo "[提示] 2) 是否需要 Git LFS（如有 .gitattributes / LFS 指针）"
echo "[提示] 3) HTTPS 鉴权是否正确（建议使用 PAT 代替密码）"
echo "[提示] 4) 改用 SSH 远端（网络/代理环境下通常更稳）"

PUSH_ARGS=("${REMOTE_NAME}" "${SOURCE_BRANCH}:${TARGET_BRANCH}" "--progress")
if [[ "${FORCE_PUSH}" == "true" ]]; then
  PUSH_ARGS+=("--force-with-lease")
fi

attempt=1
while true; do
  echo "[信息] 开始推送（第 ${attempt}/${RETRIES} 次）：git push ${PUSH_ARGS[*]}"

  set +e
  git push "${PUSH_ARGS[@]}"
  rc=$?
  set -e

  if [[ $rc -eq 0 ]]; then
    echo "[成功] 推送完成：${SOURCE_BRANCH} -> ${REMOTE_NAME}/${TARGET_BRANCH}"
    break
  fi

  if [[ $attempt -ge $RETRIES ]]; then
    echo "[失败] 推送失败并已达到最大重试次数：${RETRIES}" >&2
    echo "[提示] 建议你把日志文件发我：${LOG_FILE}" >&2
    exit $rc
  fi

  sleep_sec=$((attempt * 3))
  echo "[警告] 推送失败（退出码：${rc}），${sleep_sec}s 后重试..."
  sleep "${sleep_sec}"
  attempt=$((attempt + 1))
done
